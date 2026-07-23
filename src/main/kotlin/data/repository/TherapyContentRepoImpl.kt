package com.simbiri.data.repository

import com.simbiri.data.database.entity.therapy.*
import com.simbiri.data.database.entity.user.UserTable
import com.simbiri.data.database.utils.dbQuery
import com.simbiri.data.mapper.therapy.toEntity
import com.simbiri.data.repository.util.*
import com.simbiri.domain.model.common.*
import com.simbiri.domain.model.therapy.*
import com.simbiri.domain.repository.TherapyContentRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import java.time.Clock
import java.time.Instant
import java.util.*

class TherapyContentRepoImpl(
    private val db: Database,
    private val clock: Clock = Clock.systemUTC(),
) : TherapyContentRepository {

    // -----------------------------------------------------------------
    // Public reads
    // -----------------------------------------------------------------

    override suspend fun getTherapySessions(
        status: TherapyContentStatus?,
        authorId: UserId?,
        goal: TherapyGoal?,
        intensity: TherapyIntensity?,
        locale: String?,
    ): ResultType<List<TherapySession>, DataError> {
        val operation = "getTherapySessions"

        if (locale != null && locale.isBlank()) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "locale",
                    value = locale,
                    reason = "Locale must be omitted rather than supplied as a blank value."
                )
            )
        }

        return try {
            val sessions = db.dbQuery {
                TherapyContentAggregateReader.loadMany(
                    status = status,
                    authorId = authorId?.value,
                    goal = goal,
                    intensity = intensity,
                    locale = locale,
                )
            }

            if (sessions.isEmpty()) {
                ResultType.Failure(DataError.NotFound)
            } else {
                ResultType.Success(sessions)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "status=$status, " + "authorId=${authorId?.value}, " + "goal=$goal, " +
                            "intensity=$intensity, " + "locale=$locale"
                )
            )
        }
    }

    override suspend fun getTherapySessionById(
        therapySessionId: TherapySessionId,
    ): ResultType<TherapySession, DataError> {
        val operation = "getTherapySessionById"
        val sessionUuid = therapySessionId.value

        return try {
            val session = db.dbQuery {
                TherapyContentAggregateReader.loadById(
                    therapySessionId = sessionUuid,
                )
            }

            if (session == null) {
                ResultType.Failure(DataError.NotFound)
            } else {
                ResultType.Success(session)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation, e = e, details = "therapySessionId=$sessionUuid"
                )
            )
        }
    }

    override suspend fun getLatestTherapySessionVersion(
        seriesId: TherapySessionSeriesId,
    ): ResultType<TherapySession, DataError> {
        val operation = "getLatestTherapySessionVersion"
        val seriesUuid = seriesId.value

        return try {
            val session = db.dbQuery {
                TherapyContentAggregateReader.loadLatestBySeriesId(
                    seriesId = seriesUuid,
                )
            }

            if (session == null) {
                ResultType.Failure(DataError.NotFound)
            } else {
                ResultType.Success(session)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation, e = e, details = "seriesId=$seriesUuid"
                )
            )
        }
    }

    // -----------------------------------------------------------------
    // Draft creation
    // -----------------------------------------------------------------

    override suspend fun createDraft(
        session: TherapySession,
    ): ResultType<TherapySessionId, DataError> {
        val operation = "createDraft"

        if (session.id != null) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "session.id",
                    value = session.id.value.toString(),
                    reason = "A new therapy-session draft must not already have a persisted ID."
                )
            )
        }

        if (session.status != TherapyContentStatus.DRAFT) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "session.status",
                    value = session.status.name,
                    reason = "New therapy content must begin with DRAFT status."
                )
            )
        }

        if (session.seriesId == null && session.version != 1) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "session.version",
                    value = session.version.toString(),
                    reason = "A draft without an existing series ID must be version 1."
                )
            )
        }

        return try {
            db.dbQuery {
                validateAuthorReferenceInternal(
                    operation = operation,
                    authorId = session.authorId.value,
                )?.let { error ->
                    return@dbQuery ResultType.Failure(error)
                }

                validateDraftVersionInternal(
                    operation = operation,
                    session = session,
                )?.let { error ->
                    return@dbQuery ResultType.Failure(error)
                }

                val now = Instant.now(clock)
                val sessionEntity = session.toEntity(now)

                insertSessionRowInternal(sessionEntity)

                replaceSessionClassificationsInternal(
                    therapySessionId = sessionEntity.id,
                    goalTags = session.goalTags,
                    contraindications = session.contraindications,
                    cultureTags = session.cultureTags,
                )

                session.coverAsset?.let { coverAsset ->
                    insertAssetInternal(
                        therapySessionId = sessionEntity.id,
                        therapyModuleId = null,
                        asset = coverAsset,
                        now = now,
                    )
                }

                session.modules.sortedBy { module ->
                    module.orderIndex
                }.forEach { module ->
                    insertModuleWithAssetsInternal(
                        therapySessionId = sessionEntity.id,
                        module = module,
                        now = now,
                    )
                }

                ResultType.Success(
                    TherapySessionId(sessionEntity.id)
                )
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation, e = e, details = sessionWriteDetails(session)
                )
            )
        }
    }

    // -----------------------------------------------------------------
    // Draft details
    // -----------------------------------------------------------------

    override suspend fun updateDraftDetails(
        session: TherapySession,
    ): ResultType<Unit, DataError> {
        val operation = "updateDraftDetails"

        val sessionUuid = session.id?.value ?: return ResultType.Failure(
            validationError(
                operation = operation,
                field = "session.id",
                value = null,
                reason = "A persisted therapy-session ID is required " + "for draft updates."
            )
        )

        val seriesUuid = session.seriesId?.value ?: return ResultType.Failure(
            validationError(
                operation = operation,
                field = "session.seriesId",
                value = null,
                reason = "A persisted therapy-session series ID is required for draft updates."
            )
        )

        if (session.status != TherapyContentStatus.DRAFT) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "session.status",
                    value = session.status.name,
                    reason = "updateDraftDetails only accepts DRAFT content."
                )
            )
        }

        return try {
            db.dbQuery {
                val persisted = TherapyContentAggregateReader.loadById(
                    therapySessionId = sessionUuid,
                ) ?: return@dbQuery ResultType.Failure(
                    DataError.NotFound
                )

                validatePersistedDraftInternal(
                    operation = operation,
                    session = persisted,
                )?.let { error ->
                    return@dbQuery ResultType.Failure(error)
                }

                validateImmutableDraftFieldsInternal(
                    operation = operation,
                    incoming = session,
                    persisted = persisted,
                )?.let { error ->
                    return@dbQuery ResultType.Failure(error)
                }

                if (persisted.seriesId?.value != seriesUuid) {
                    return@dbQuery ResultType.Failure(
                        conflictError(
                            operation = operation,
                            message = "Therapy-session series mismatch. therapySessionId=$sessionUuid, " +
                                    "persistedSeriesId=${persisted.seriesId?.value}, requestedSeriesId=$seriesUuid."
                        )
                    )
                }

                session.coverAsset?.let { coverAsset ->
                    validateCoverStorageKeyInternal(
                        operation = operation,
                        therapySessionId = sessionUuid,
                        coverAsset = coverAsset,
                    )?.let { error ->
                        return@dbQuery ResultType.Failure(error)
                    }
                }

                val now = Instant.now(clock)

                TherapySessionTable.update(
                    where = {
                        TherapySessionTable.id eq sessionUuid
                    }) { row ->
                    row[title] = session.title.trim()
                    row[description] = session.description.trim()
                    row[tagline] = session.tagline?.trim()
                    row[therapeuticPriority] = session.therapeuticPriority.name
                    row[intensity] = session.intensity.name
                    row[locale] = session.locale.trim()
                    row[updatedAt] = now
                }

                replaceSessionClassificationsInternal(
                    therapySessionId = sessionUuid,
                    goalTags = session.goalTags,
                    contraindications = session.contraindications,
                    cultureTags = session.cultureTags,
                )

                replaceCoverAssetInternal(
                    therapySessionId = sessionUuid,
                    coverAsset = session.coverAsset,
                    now = now,
                )

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation, e = e, details = sessionWriteDetails(session)
                )
            )
        }
    }

    // -----------------------------------------------------------------
    // Module management
    // -----------------------------------------------------------------

    override suspend fun addModule(
        therapySessionId: TherapySessionId,
        module: TherapyModule,
    ): ResultType<TherapyModuleId, DataError> {
        val operation = "addModule"
        val sessionUuid = therapySessionId.value

        if (module.id != null) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "module.id",
                    value = module.id.value.toString(),
                    reason = "A new therapy module must not already have a persisted ID."
                )
            )
        }

        return try {
            db.dbQuery {
                val persistedSession = TherapyContentAggregateReader.loadById(
                    therapySessionId = sessionUuid,
                ) ?: return@dbQuery ResultType.Failure(
                    DataError.NotFound
                )

                validatePersistedDraftInternal(
                    operation = operation,
                    session = persistedSession,
                )?.let { error ->
                    return@dbQuery ResultType.Failure(error)
                }

                val existingModules = loadModuleOrderInternal(
                    therapySessionId = sessionUuid,
                )

                if (module.orderIndex !in 0..existingModules.size) {
                    return@dbQuery ResultType.Failure(
                        validationError(
                            operation = operation,
                            field = "module.orderIndex",
                            value = module.orderIndex.toString(),
                            reason = "New module order index must be between 0 and ${existingModules.size}, inclusive."
                        )
                    )
                }

                validateModuleStorageKeysInternal(
                    operation = operation,
                    therapySessionId = sessionUuid,
                    incomingAssets = module.assets,
                    excludedModuleId = null,
                )?.let { error ->
                    return@dbQuery ResultType.Failure(error)
                }

                shiftModulesUpInternal(
                    therapySessionId = sessionUuid,
                    startingAt = module.orderIndex,
                )

                val now = Instant.now(clock)

                val moduleEntity = insertModuleWithAssetsInternal(
                    therapySessionId = sessionUuid,
                    module = module,
                    now = now,
                )

                touchSessionInternal(
                    therapySessionId = sessionUuid,
                    now = now,
                )

                ResultType.Success(
                    TherapyModuleId(moduleEntity.id)
                )
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "therapySessionId=$sessionUuid, " + moduleWriteDetails(module)
                )
            )
        }
    }

    override suspend fun updateModule(
        therapySessionId: TherapySessionId,
        module: TherapyModule,
    ): ResultType<Unit, DataError> {
        val operation = "updateModule"
        val sessionUuid = therapySessionId.value

        val moduleUuid = module.id?.value ?: return ResultType.Failure(
            validationError(
                operation = operation,
                field = "module.id",
                value = null,
                reason = "A persisted therapy-module ID is required " + "for update."
            )
        )

        return try {
            db.dbQuery {
                val persistedSession = TherapyContentAggregateReader.loadById(
                    therapySessionId = sessionUuid,
                ) ?: return@dbQuery ResultType.Failure(
                    DataError.NotFound
                )

                validatePersistedDraftInternal(
                    operation = operation,
                    session = persistedSession,
                )?.let { error ->
                    return@dbQuery ResultType.Failure(error)
                }

                val moduleRow = TherapyModuleTable.selectAll().where {
                    (TherapyModuleTable.id eq moduleUuid) and (TherapyModuleTable.therapySessionId eq sessionUuid)
                }.singleOrNull() ?: return@dbQuery ResultType.Failure(
                    DataError.NotFound
                )

                val persistedOrderIndex = moduleRow[TherapyModuleTable.orderIndex]

                if (module.orderIndex != persistedOrderIndex) {
                    return@dbQuery ResultType.Failure(
                        conflictError(
                            operation = operation,
                            message = "updateModule cannot change module ordering. therapySessionId=$sessionUuid, " +
                                    "therapyModuleId=$moduleUuid, " + "persistedOrderIndex=$persistedOrderIndex, " +
                                    "requestedOrderIndex=${module.orderIndex}. Use reorderModules for ordering changes."
                        )
                    )
                }

                validateModuleStorageKeysInternal(
                    operation = operation,
                    therapySessionId = sessionUuid,
                    incomingAssets = module.assets,
                    excludedModuleId = moduleUuid,
                )?.let { error ->
                    return@dbQuery ResultType.Failure(error)
                }

                val now = Instant.now(clock)

                TherapyModuleTable.update(
                    where = {
                        (TherapyModuleTable.id eq moduleUuid) and (TherapyModuleTable.therapySessionId eq sessionUuid)
                    }) { row ->
                    row[title] = module.title.trim()
                    row[goal] = module.goal.trim()
                    row[instructions] = module.instructions.trim()
                    row[whyThisHelps] = module.whyThisHelps.trim()
                    row[modality] = module.modality.name
                    row[estimatedDurationSeconds] = module.estimatedDurationSeconds
                    row[isSkippable] = module.isSkippable
                    row[isRepeatable] = module.isRepeatable
                    row[updatedAt] = now
                }

                replaceModuleAssetsInternal(
                    therapySessionId = sessionUuid,
                    therapyModuleId = moduleUuid,
                    assets = module.assets,
                    now = now,
                )

                touchSessionInternal(
                    therapySessionId = sessionUuid,
                    now = now,
                )

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "therapySessionId=$sessionUuid, therapyModuleId=$moduleUuid, " + moduleWriteDetails(
                        module
                    )
                )
            )
        }
    }

    override suspend fun reorderModules(
        therapySessionId: TherapySessionId,
        orderedModuleIds: List<TherapyModuleId>,
    ): ResultType<Unit, DataError> {
        val operation = "reorderModules"
        val sessionUuid = therapySessionId.value

        val duplicateModuleId = orderedModuleIds.groupingBy { moduleId ->
            moduleId
        }.eachCount().entries.firstOrNull { (_, count) ->
            count > 1
        }?.key

        if (duplicateModuleId != null) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "orderedModuleIds",
                    value = duplicateModuleId.value.toString(),
                    reason = "The module ordering cannot contain duplicate module IDs."
                )
            )
        }

        return try {
            db.dbQuery {
                val persistedSession = TherapyContentAggregateReader.loadById(
                    therapySessionId = sessionUuid,
                ) ?: return@dbQuery ResultType.Failure(
                    DataError.NotFound
                )

                validatePersistedDraftInternal(
                    operation = operation,
                    session = persistedSession,
                )?.let { error ->
                    return@dbQuery ResultType.Failure(error)
                }

                val persistedModuleIds = loadModuleOrderInternal(
                    therapySessionId = sessionUuid,
                ).map { pair ->
                    pair.first
                }

                val requestedModuleUuids = orderedModuleIds.map { moduleId ->
                    moduleId.value
                }

                if (persistedModuleIds.toSet() != requestedModuleUuids.toSet()) {
                    val missingIds = persistedModuleIds.toSet() - requestedModuleUuids.toSet()

                    val unexpectedIds = requestedModuleUuids.toSet() - persistedModuleIds.toSet()

                    return@dbQuery ResultType.Failure(
                        conflictError(
                            operation = operation,
                            message = "The supplied ordering must contain " + "every existing module exactly once. " + "therapySessionId=$sessionUuid, " + "persistedModuleIds=$persistedModuleIds, " + "requestedModuleIds=$requestedModuleUuids, " + "missingIds=$missingIds, " + "unexpectedIds=$unexpectedIds."
                        )
                    )
                }

                /*
                 * First move every module to a unique negative index.
                 * This avoids violating the unique
                 * (therapy_session_id, order_index) constraint while
                 * applying the final zero-based ordering.
                 */
                requestedModuleUuids.forEachIndexed { index, moduleUuid ->
                    TherapyModuleTable.update(
                        where = {
                            (TherapyModuleTable.id eq moduleUuid) and (TherapyModuleTable.therapySessionId eq sessionUuid)
                        }) { row ->
                        row[orderIndex] = -(index + 1)
                    }
                }

                val now = Instant.now(clock)

                requestedModuleUuids.forEachIndexed { index, moduleUuid ->
                    TherapyModuleTable.update(
                        where = {
                            (TherapyModuleTable.id eq moduleUuid) and (TherapyModuleTable.therapySessionId eq sessionUuid)
                        }) { row ->
                        row[orderIndex] = index
                        row[updatedAt] = now
                    }
                }

                touchSessionInternal(
                    therapySessionId = sessionUuid,
                    now = now,
                )

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "therapySessionId=$sessionUuid, " + "orderedModuleIds=" + "${orderedModuleIds.map { it.value }}"
                )
            )
        }
    }

    override suspend fun removeModule(
        therapySessionId: TherapySessionId,
        therapyModuleId: TherapyModuleId,
    ): ResultType<Unit, DataError> {
        val operation = "removeModule"
        val sessionUuid = therapySessionId.value
        val moduleUuid = therapyModuleId.value

        return try {
            db.dbQuery {
                val persistedSession = TherapyContentAggregateReader.loadById(
                    therapySessionId = sessionUuid,
                ) ?: return@dbQuery ResultType.Failure(
                    DataError.NotFound
                )

                validatePersistedDraftInternal(
                    operation = operation,
                    session = persistedSession,
                )?.let { error ->
                    return@dbQuery ResultType.Failure(error)
                }

                val moduleRow = TherapyModuleTable.selectAll().where {
                    (TherapyModuleTable.id eq moduleUuid) and (TherapyModuleTable.therapySessionId eq sessionUuid)
                }.singleOrNull() ?: return@dbQuery ResultType.Failure(
                    DataError.NotFound
                )

                val removedOrderIndex = moduleRow[TherapyModuleTable.orderIndex]

                TherapyAssetTable.deleteWhere {
                    TherapyAssetTable.therapyModuleId eq moduleUuid
                }

                TherapyModuleTable.deleteWhere {
                    (TherapyModuleTable.id eq moduleUuid) and (TherapyModuleTable.therapySessionId eq sessionUuid)
                }

                compactModuleOrderInternal(
                    therapySessionId = sessionUuid,
                    removedOrderIndex = removedOrderIndex,
                )

                touchSessionInternal(
                    therapySessionId = sessionUuid,
                    now = Instant.now(clock),
                )

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "therapySessionId=$sessionUuid, " + "therapyModuleId=$moduleUuid"
                )
            )
        }
    }

    // -----------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------

    override suspend fun transitionStatus(
        therapySessionId: TherapySessionId,
        expectedStatus: TherapyContentStatus,
        targetStatus: TherapyContentStatus,
        transitionedAt: Timestamp,
    ): ResultType<Unit, DataError> {
        val operation = "transitionStatus"
        val sessionUuid = therapySessionId.value

        return try {
            db.dbQuery {
                val updatedCount = TherapySessionTable.update(
                    where = {
                        (TherapySessionTable.id eq sessionUuid) and (TherapySessionTable.status eq expectedStatus.name)
                    }) { row ->
                    row[status] = targetStatus.name
                    row[updatedAt] = transitionedAt

                    when (targetStatus) {
                        TherapyContentStatus.DRAFT, TherapyContentStatus.IN_REVIEW -> {
                            row[publishedAt] = null
                            row[archivedAt] = null
                        }

                        TherapyContentStatus.PUBLISHED -> {
                            row[publishedAt] = transitionedAt
                            row[archivedAt] = null
                        }

                        TherapyContentStatus.ARCHIVED -> {
                            row[archivedAt] = transitionedAt
                        }
                    }
                }

                if (updatedCount > 0) {
                    return@dbQuery ResultType.Success(Unit)
                }

                val currentStatusName = TherapySessionTable.selectAll().where {
                    TherapySessionTable.id eq sessionUuid
                }.singleOrNull()?.get(TherapySessionTable.status) ?: return@dbQuery ResultType.Failure(
                    DataError.NotFound
                )

                ResultType.Failure(
                    conflictError(
                        operation = operation,
                        message = "Therapy-content status transition lost " + "its compare-and-set condition. " + "therapySessionId=$sessionUuid, " + "expectedStatus=$expectedStatus, " + "currentPersistedStatus=$currentStatusName, " + "targetStatus=$targetStatus. " + "The content may have been changed by " + "another request."
                    )
                )
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "therapySessionId=$sessionUuid, " + "expectedStatus=$expectedStatus, " + "targetStatus=$targetStatus, " + "transitionedAt=$transitionedAt"
                )
            )
        }
    }

    override suspend fun deleteDraft(
        therapySessionId: TherapySessionId,
    ): ResultType<Unit, DataError> {
        val operation = "deleteDraft"
        val sessionUuid = therapySessionId.value

        return try {
            db.dbQuery {
                val statusName = TherapySessionTable.selectAll().where {
                    TherapySessionTable.id eq sessionUuid
                }.singleOrNull()?.get(TherapySessionTable.status) ?: return@dbQuery ResultType.Failure(
                    DataError.NotFound
                )

                if (statusName != TherapyContentStatus.DRAFT.name) {
                    return@dbQuery ResultType.Failure(
                        conflictError(
                            operation = operation,
                            message = "Only DRAFT therapy content may be " + "permanently deleted. " + "therapySessionId=$sessionUuid, " + "persistedStatus=$statusName. " + "Published content must be archived."
                        )
                    )
                }

                TherapyAssetTable.deleteWhere {
                    TherapyAssetTable.therapySessionId eq sessionUuid
                }

                TherapySessionGoalTable.deleteWhere {
                    TherapySessionGoalTable.therapySessionId eq sessionUuid
                }

                TherapySessionContraindicationTable.deleteWhere {
                    TherapySessionContraindicationTable.therapySessionId eq sessionUuid
                }

                TherapySessionCultureTagTable.deleteWhere {
                    TherapySessionCultureTagTable.therapySessionId eq sessionUuid
                }

                TherapyModuleTable.deleteWhere {
                    TherapyModuleTable.therapySessionId eq sessionUuid
                }

                val deletedCount = TherapySessionTable.deleteWhere {
                    TherapySessionTable.id eq sessionUuid
                }

                if (deletedCount == 0) {
                    ResultType.Failure(DataError.NotFound)
                } else {
                    ResultType.Success(Unit)
                }
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation, e = e, details = "therapySessionId=$sessionUuid"
                )
            )
        }
    }

    // -----------------------------------------------------------------
    // Reference and state validation
    //
    // All helpers below must run inside dbQuery.
    // -----------------------------------------------------------------

    private fun validateAuthorReferenceInternal(
        operation: String,
        authorId: UUID,
    ): DataError? {
        val exists = UserTable.selectAll().where {
            UserTable.id eq authorId
        }.limit(1).any()

        if (exists) {
            return null
        }

        return foreignKeyError(
            operation = operation, message = "Therapy-session author does not exist.\n authorId=$authorId."
        )
    }

    private fun validateDraftVersionInternal(
        operation: String,
        session: TherapySession,
    ): DataError? {
        val requestedSeriesId = session.seriesId?.value

        if (requestedSeriesId == null) {
            if (session.version == 1) {
                return null
            }

            return validationError(
                operation = operation,
                field = "session.version",
                value = session.version.toString(),
                reason = "A new therapy-session series must begin at version 1."
            )
        }

        val existingVersions = TherapySessionTable.selectAll().where {
            TherapySessionTable.seriesId eq requestedSeriesId
        }.orderBy(
            TherapySessionTable.version to SortOrder.DESC
        ).toList()

        if (existingVersions.isEmpty()) {
            if (session.version == 1) {
                return null
            }

            return foreignKeyError(
                operation = operation,
                message = "Therapy-session series does not exist. " + "seriesId=$requestedSeriesId, " +
                        "requestedVersion=${session.version}. " +
                        "A non-existing series may only be created as " + "version 1."
            )
        }

        val latestRow = existingVersions.first()
        val latestVersion = latestRow[TherapySessionTable.version]

        val latestStatus = TherapyContentStatus.valueOf(
            latestRow[TherapySessionTable.status]
        )

        val latestAuthorId = latestRow[TherapySessionTable.authorId]

        if (latestAuthorId != session.authorId.value) {
            return conflictError(
                operation = operation,
                message = "A later therapy-session version must retain the original series author. "
                        + "seriesId=$requestedSeriesId, " + "persistedAuthorId=$latestAuthorId, "
                        + "requestedAuthorId=${session.authorId.value}."
            )
        }

        if (latestStatus == TherapyContentStatus.DRAFT || latestStatus == TherapyContentStatus.IN_REVIEW) {
            return conflictError(
                operation = operation,
                message = "A new therapy-session version cannot be created "
                        + "while the current latest version is still editable or under review. "
                        + "seriesId=$requestedSeriesId, " + "latestVersion=$latestVersion, " + "latestStatus=$latestStatus."
            )
        }

        val expectedVersion = latestVersion + 1

        if (session.version != expectedVersion) {
            return duplicateResourceError(
                operation = operation,
                message = "Therapy-session version is not the next available series version. "
                        + "seriesId=$requestedSeriesId, " + "latestVersion=$latestVersion, "
                        + "expectedVersion=$expectedVersion, " + "requestedVersion=${session.version}."
            )
        }

        return null
    }

    private fun validatePersistedDraftInternal(
        operation: String,
        session: TherapySession,
    ): DataError? {
        if (session.status == TherapyContentStatus.DRAFT) {
            return null
        }

        return conflictError(
            operation = operation,
            message = "Therapy content is immutable in its current state. therapySessionId=${session.id?.value}, "
                    + "status=${session.status}. Only DRAFT content may be modified."
        )
    }

    private fun validateImmutableDraftFieldsInternal(
        operation: String,
        incoming: TherapySession,
        persisted: TherapySession,
    ): DataError? = when {
        incoming.authorId != persisted.authorId -> {
            conflictError(
                operation = operation,
                message = "Therapy-session author cannot be changed through updateDraftDetails. "
                        + "therapySessionId=${persisted.id?.value}, " + "persistedAuthorId=${persisted.authorId.value}, "
                        + "requestedAuthorId=${incoming.authorId.value}."
            )
        }

        incoming.seriesId != persisted.seriesId -> {
            conflictError(
                operation = operation,
                message = "Therapy-session series ID is immutable. " + "therapySessionId=${persisted.id?.value}, "
                        + "persistedSeriesId=${persisted.seriesId?.value}, " + "requestedSeriesId=${incoming.seriesId?.value}."
            )
        }

        incoming.version != persisted.version -> {
            conflictError(
                operation = operation,
                message = "Therapy-session version cannot be changed in place. "
                        + "therapySessionId=${persisted.id?.value}, " + "persistedVersion=${persisted.version}, "
                        + "requestedVersion=${incoming.version}."
            )
        }

        else -> null
    }

    private fun validateCoverStorageKeyInternal(
        operation: String,
        therapySessionId: UUID,
        coverAsset: TherapyAsset,
    ): DataError? {
        if (coverAsset.role != TherapyAssetRole.SESSION_COVER) {
            return validationError(
                operation = operation,
                field = "coverAsset.role",
                value = coverAsset.role.name,
                reason = "The session cover must use SESSION_COVER role."
            )
        }

        val normalizedStorageKey = coverAsset.storageKey.trim().lowercase()

        val conflictingModuleAsset = TherapyAssetTable.selectAll().where {
            TherapyAssetTable.therapySessionId eq therapySessionId
        }.firstOrNull { row ->
            row[TherapyAssetTable.therapyModuleId] != null && row[TherapyAssetTable.storageKey].trim()
                .lowercase() == normalizedStorageKey
        }

        if (conflictingModuleAsset == null) {
            return null
        }

        return duplicateResourceError(
            operation = operation,
            message = "Cover asset storage key is already used by a module asset in this therapy session. "
                    + "therapySessionId=$therapySessionId, " + "storageKey=${coverAsset.storageKey}, "
                    + "conflictingAssetId=" + "${conflictingModuleAsset[TherapyAssetTable.id].value}, "
                    + "conflictingModuleId=" + "${conflictingModuleAsset[TherapyAssetTable.therapyModuleId]}."
        )
    }

    private fun validateModuleStorageKeysInternal(
        operation: String,
        therapySessionId: UUID,
        incomingAssets: List<TherapyAsset>,
        excludedModuleId: UUID?,
    ): DataError? {
        val duplicateIncomingKey = incomingAssets.groupingBy { asset ->
            asset.storageKey.trim().lowercase()
        }.eachCount().entries.firstOrNull { (_, count) ->
            count > 1
        }?.key

        if (duplicateIncomingKey != null) {
            return duplicateResourceError(
                operation = operation,
                message = "The module payload references the same asset storage key more than once. "
                        + "therapySessionId=$therapySessionId, " + "storageKey=$duplicateIncomingKey."
            )
        }

        val existingRows = TherapyAssetTable.selectAll().where {
            TherapyAssetTable.therapySessionId eq therapySessionId
        }.toList().filter { row ->
            val persistedModuleId = row[TherapyAssetTable.therapyModuleId]

            excludedModuleId == null || persistedModuleId != excludedModuleId
        }

        val existingByNormalizedKey = existingRows.associateBy { row ->
            row[TherapyAssetTable.storageKey].trim().lowercase()
        }

        val conflict = incomingAssets.firstNotNullOfOrNull { asset ->
            val normalizedKey = asset.storageKey.trim().lowercase()

            existingByNormalizedKey[normalizedKey]?.let { row ->
                asset to row
            }
        }

        if (conflict == null) {
            return null
        }

        val incomingAsset = conflict.first
        val persistedRow = conflict.second

        return duplicateResourceError(
            operation = operation,
            message = "Therapy asset storage key is already used within the same TherapySession aggregate. "
                    + "therapySessionId=$therapySessionId, " + "storageKey=${incomingAsset.storageKey}, "
                    + "conflictingAssetId=${persistedRow[TherapyAssetTable.id].value}, "
                    + "conflictingModuleId=${persistedRow[TherapyAssetTable.therapyModuleId]}."
        )
    }

    // -----------------------------------------------------------------
    // Session writes
    // -----------------------------------------------------------------

    private fun insertSessionRowInternal(
        entity: TherapySessionEntity,
    ) {
        TherapySessionTable.insert { row ->
            row[TherapySessionTable.id] = entity.id
            row[seriesId] = entity.seriesId
            row[authorId] = entity.authorId
            row[title] = entity.title
            row[description] = entity.description
            row[tagline] = entity.tagline
            row[status] = entity.statusName
            row[version] = entity.version
            row[therapeuticPriority] = entity.therapeuticPriorityName
            row[intensity] = entity.intensityName
            row[locale] = entity.locale
            row[createdAt] = entity.createdAt
            row[updatedAt] = entity.updatedAt
            row[publishedAt] = entity.publishedAt
            row[archivedAt] = entity.archivedAt
        }
    }

    private fun replaceSessionClassificationsInternal(
        therapySessionId: UUID,
        goalTags: Set<TherapyGoal>,
        contraindications: Set<TherapyContraindication>,
        cultureTags: Set<String>,
    ) {
        TherapySessionGoalTable.deleteWhere {
            TherapySessionGoalTable.therapySessionId eq therapySessionId
        }

        goalTags.forEach { goal ->
            TherapySessionGoalTable.insert { row ->
                row[TherapySessionGoalTable.therapySessionId] = therapySessionId

                row[TherapySessionGoalTable.goal] = goal.name
            }
        }

        TherapySessionContraindicationTable.deleteWhere {
            TherapySessionContraindicationTable.therapySessionId eq therapySessionId
        }

        contraindications.forEach { contraindication ->
            TherapySessionContraindicationTable.insert { row ->
                row[TherapySessionContraindicationTable.therapySessionId] = therapySessionId

                row[TherapySessionContraindicationTable.contraindication] = contraindication.name
            }
        }

        TherapySessionCultureTagTable.deleteWhere {
            TherapySessionCultureTagTable.therapySessionId eq therapySessionId
        }

        cultureTags.forEach { cultureTag ->
            val normalized = cultureTag.trim()

            TherapySessionCultureTagTable.insert { row ->
                row[TherapySessionCultureTagTable.therapySessionId] = therapySessionId

                row[TherapySessionCultureTagTable.tag] = normalized

                row[TherapySessionCultureTagTable.normalizedTag] = normalized.lowercase()
            }
        }
    }

    private fun touchSessionInternal(
        therapySessionId: UUID,
        now: Instant,
    ) {
        TherapySessionTable.update(
            where = {
                TherapySessionTable.id eq therapySessionId
            }) { row ->
            row[updatedAt] = now
        }
    }

    // -----------------------------------------------------------------
    // Module writes
    // -----------------------------------------------------------------

    private fun insertModuleWithAssetsInternal(
        therapySessionId: UUID,
        module: TherapyModule,
        now: Instant,
    ): TherapyModuleEntity {
        val moduleEntity = module.toEntity(
            therapySessionId = therapySessionId,
            now = now,
        )

        TherapyModuleTable.insert { row ->
            row[TherapyModuleTable.id] = moduleEntity.id
            row[TherapyModuleTable.therapySessionId] = therapySessionId
            row[orderIndex] = moduleEntity.orderIndex
            row[title] = moduleEntity.title
            row[goal] = moduleEntity.goal
            row[instructions] = moduleEntity.instructions
            row[whyThisHelps] = moduleEntity.whyThisHelps
            row[modality] = moduleEntity.modalityName
            row[estimatedDurationSeconds] = moduleEntity.estimatedDurationSeconds
            row[isSkippable] = moduleEntity.isSkippable
            row[isRepeatable] = moduleEntity.isRepeatable
            row[createdAt] = moduleEntity.createdAt
            row[updatedAt] = moduleEntity.updatedAt
        }

        module.assets.forEach { asset ->
            insertAssetInternal(
                therapySessionId = therapySessionId,
                therapyModuleId = moduleEntity.id,
                asset = asset,
                now = now,
            )
        }

        return moduleEntity
    }

    private fun loadModuleOrderInternal(
        therapySessionId: UUID,
    ): List<Pair<UUID, Int>> = TherapyModuleTable.selectAll().where {
        TherapyModuleTable.therapySessionId eq therapySessionId
    }.orderBy(
        TherapyModuleTable.orderIndex to SortOrder.ASC
    ).map { row ->
        row[TherapyModuleTable.id].value to row[TherapyModuleTable.orderIndex]
    }

    private fun shiftModulesUpInternal(
        therapySessionId: UUID,
        startingAt: Int,
    ) {
        val modulesToShift = TherapyModuleTable.selectAll().where {
            TherapyModuleTable.therapySessionId eq therapySessionId
        }.orderBy(
            TherapyModuleTable.orderIndex to SortOrder.DESC
        ).map { row ->
            row[TherapyModuleTable.id].value to row[TherapyModuleTable.orderIndex]
        }.filter { (_, orderIndex) ->
            orderIndex >= startingAt
        }

        modulesToShift.forEach { (moduleId, orderIndex) ->
            TherapyModuleTable.update(
                where = {
                    TherapyModuleTable.id eq moduleId
                }) { row ->
                row[TherapyModuleTable.orderIndex] = orderIndex + 1
            }
        }
    }

    private fun compactModuleOrderInternal(
        therapySessionId: UUID,
        removedOrderIndex: Int,
    ) {
        val modulesToShift = TherapyModuleTable.selectAll().where {
            TherapyModuleTable.therapySessionId eq therapySessionId
        }.orderBy(
            TherapyModuleTable.orderIndex to SortOrder.ASC
        ).map { row ->
            row[TherapyModuleTable.id].value to row[TherapyModuleTable.orderIndex]
        }.filter { (_, orderIndex) ->
            orderIndex > removedOrderIndex
        }

        modulesToShift.forEach { (moduleId, orderIndex) ->
            TherapyModuleTable.update(
                where = {
                    TherapyModuleTable.id eq moduleId
                }) { row ->
                row[TherapyModuleTable.orderIndex] = orderIndex - 1
            }
        }
    }

    // -----------------------------------------------------------------
    // Asset writes
    // -----------------------------------------------------------------

    private fun insertAssetInternal(
        therapySessionId: UUID,
        therapyModuleId: UUID?,
        asset: TherapyAsset,
        now: Instant,
    ) {
        val assetEntity = asset.toEntity(
            therapySessionId = therapySessionId,
            therapyModuleId = therapyModuleId,
            now = now,
        )

        TherapyAssetTable.insert { row ->
            row[TherapyAssetTable.id] = assetEntity.id
            row[TherapyAssetTable.therapySessionId] = assetEntity.therapySessionId
            row[TherapyAssetTable.therapyModuleId] = assetEntity.therapyModuleId
            row[role] = assetEntity.roleName
            row[mediaType] = assetEntity.mediaTypeName
            row[storageKey] = assetEntity.storageKey
            row[mimeType] = assetEntity.mimeType
            row[sizeBytes] = assetEntity.sizeBytes
            row[sha256] = assetEntity.sha256
            row[locale] = assetEntity.locale
            row[altText] = assetEntity.altText
            row[transcript] = assetEntity.transcript
            row[createdAt] = assetEntity.createdAt
            row[updatedAt] = assetEntity.updatedAt
        }
    }

    private fun replaceCoverAssetInternal(
        therapySessionId: UUID,
        coverAsset: TherapyAsset?,
        now: Instant,
    ) {
        TherapyAssetTable.deleteWhere {
            (TherapyAssetTable.therapySessionId eq therapySessionId) and TherapyAssetTable.therapyModuleId.isNull()
        }

        coverAsset?.let { asset ->
            insertAssetInternal(
                therapySessionId = therapySessionId,
                therapyModuleId = null,
                asset = asset,
                now = now,
            )
        }
    }

    private fun replaceModuleAssetsInternal(
        therapySessionId: UUID,
        therapyModuleId: UUID,
        assets: List<TherapyAsset>,
        now: Instant,
    ) {
        TherapyAssetTable.deleteWhere {
            TherapyAssetTable.therapyModuleId eq therapyModuleId
        }

        assets.forEach { asset ->
            insertAssetInternal(
                therapySessionId = therapySessionId,
                therapyModuleId = therapyModuleId,
                asset = asset,
                now = now,
            )
        }
    }

    // -----------------------------------------------------------------
    // Error context
    // -----------------------------------------------------------------

    private fun sessionWriteDetails(
        session: TherapySession,
    ): String =
        "therapySessionId=${session.id?.value}, " +
                "seriesId=${session.seriesId?.value}, " +
                "authorId=${session.authorId.value}, " +
                "title=${session.title}, " +
                "status=${session.status}, " +
                "version=${session.version}, " +
                "moduleCount=${session.modules.size}, " +
                "goalCount=${session.goalTags.size}, " +
                "contraindicationCount=" +
                "${session.contraindications.size}, " +
                "cultureTagCount=${session.cultureTags.size}, " +
                "hasCoverAsset=${session.coverAsset != null}"

    private fun moduleWriteDetails(
        module: TherapyModule,
    ): String =
        "therapyModuleId=${module.id?.value}, " +
                "orderIndex=${module.orderIndex}, " +
                "title=${module.title}, " +
                "modality=${module.modality}, " +
                "estimatedDurationSeconds=" +
                "${module.estimatedDurationSeconds}, " +
                "assetCount=${module.assets.size}"
}
