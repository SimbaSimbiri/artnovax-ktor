package com.simbiri.data.repository

import com.simbiri.data.database.entity.therapy.TherapyAssetTable
import com.simbiri.data.database.entity.therapy.TherapyModuleTable
import com.simbiri.data.database.entity.therapy.TherapySessionTable
import com.simbiri.data.database.utils.dbQuery
import com.simbiri.domain.model.common.TherapyAssetId
import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.therapy.TherapyAsset
import com.simbiri.domain.model.therapy.TherapyAssetRole
import com.simbiri.domain.model.therapy.TherapyContentStatus
import com.simbiri.domain.model.therapy.TherapyMediaType
import com.simbiri.domain.repository.TherapyAssetReplacementResult
import com.simbiri.domain.repository.TherapyAssetRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.util.Locale
import java.util.UUID

class TherapyAssetRepoImpl(
    private val db: Database,
    private val clock: Clock = Clock.systemUTC(),
) : TherapyAssetRepository {

    override suspend fun replaceAsset(
        therapySessionId: TherapySessionId,
        therapyModuleId: TherapyModuleId?,
        asset: TherapyAsset,
    ): ResultType<TherapyAssetReplacementResult, DataError> {
        val operation = "replaceTherapyAsset"
        val sessionUuid = therapySessionId.value
        val moduleUuid = therapyModuleId?.value

        if (asset.id != null) {
            return ResultType.Failure(
                DataError.ValidationError(
                    message = "$operation failed. A replacement asset must not already have a persisted ID."
                )
            )
        }

        if (moduleUuid == null && asset.role != TherapyAssetRole.SESSION_COVER) {
            return ResultType.Failure(
                DataError.ValidationError(
                    message = "$operation failed. A session-level asset must use SESSION_COVER role."
                )
            )
        }

        if (moduleUuid != null && asset.role == TherapyAssetRole.SESSION_COVER) {
            return ResultType.Failure(
                DataError.ValidationError(
                    message = "$operation failed. A module asset cannot use SESSION_COVER role."
                )
            )
        }

        return try {
            db.dbQuery {
                val sessionStatus = TherapySessionTable.selectAll()
                    .where { TherapySessionTable.id eq sessionUuid }
                    .singleOrNull()
                    ?.get(TherapySessionTable.status)
                    ?: return@dbQuery ResultType.Failure(DataError.NotFound)

                if (sessionStatus != TherapyContentStatus.DRAFT.name) {
                    return@dbQuery ResultType.Failure(
                        DataError.Conflict(
                            message = "$operation failed. Only DRAFT therapy content can replace assets. " +
                                    "therapySessionId=$sessionUuid, status=$sessionStatus."
                        )
                    )
                }

                if (moduleUuid != null) {
                    val moduleExists = TherapyModuleTable.selectAll()
                        .where {
                            (TherapyModuleTable.id eq moduleUuid) and
                                    (TherapyModuleTable.therapySessionId eq sessionUuid)
                        }
                        .limit(1)
                        .any()

                    if (!moduleExists) {
                        return@dbQuery ResultType.Failure(DataError.NotFound)
                    }
                }

                val sessionAssetRows = TherapyAssetTable.selectAll()
                    .where { TherapyAssetTable.therapySessionId eq sessionUuid }
                    .toList()

                val replacementRows = sessionAssetRows.filter { row ->
                    row.belongsToReplacementSlot(
                        therapyModuleId = moduleUuid,
                        asset = asset,
                    )
                }

                val normalizedStorageKey = asset.storageKey.trim().lowercase(Locale.ROOT)
                val existingStorageKeyRow = sessionAssetRows.firstOrNull { row ->
                    row[TherapyAssetTable.storageKey].trim().lowercase(Locale.ROOT) == normalizedStorageKey
                }

                if (existingStorageKeyRow != null) {
                    val replacementIds = replacementRows.map { row ->
                        row[TherapyAssetTable.id].value
                    }.toSet()

                    val existingAssetId = existingStorageKeyRow[TherapyAssetTable.id].value

                    if (existingAssetId in replacementIds && existingStorageKeyRow.matches(asset)) {
                        return@dbQuery ResultType.Success(
                            TherapyAssetReplacementResult(
                                therapyAssetId = TherapyAssetId(existingAssetId),
                                replacedStorageKeys = emptySet(),
                            )
                        )
                    }

                    return@dbQuery ResultType.Failure(
                        DataError.DuplicateResource(
                            message = "$operation failed. The storage key is already attached to this therapy " +
                                    "session. therapySessionId=$sessionUuid, storageKey=${asset.storageKey}."
                        )
                    )
                }

                val replacedStorageKeys = replacementRows.map { row ->
                    row[TherapyAssetTable.storageKey]
                }.toSet()

                replacementRows.forEach { row ->
                    val therapyAssetUuid = row[TherapyAssetTable.id].value

                    TherapyAssetTable.deleteWhere {
                        TherapyAssetTable.id eq therapyAssetUuid
                    }
                }

                val now = clock.instant()
                val therapyAssetUuid = UUID.randomUUID()

                TherapyAssetTable.insert { row ->
                    row[TherapyAssetTable.id] = therapyAssetUuid
                    row[TherapyAssetTable.therapySessionId] = sessionUuid
                    row[TherapyAssetTable.therapyModuleId] = moduleUuid
                    row[TherapyAssetTable.role] = asset.role.name
                    row[TherapyAssetTable.mediaType] = asset.mediaType.name
                    row[TherapyAssetTable.storageKey] = asset.storageKey.trim()
                    row[TherapyAssetTable.mimeType] = asset.mimeType.trim().lowercase(Locale.ROOT)
                    row[TherapyAssetTable.sizeBytes] = asset.sizeBytes
                    row[TherapyAssetTable.sha256] = asset.sha256.trim().lowercase(Locale.ROOT)
                    row[TherapyAssetTable.locale] = asset.locale?.trim()
                    row[TherapyAssetTable.altText] = asset.altText?.trim()
                    row[TherapyAssetTable.transcript] = asset.transcript?.trim()
                    row[TherapyAssetTable.createdAt] = now
                    row[TherapyAssetTable.updatedAt] = now
                }

                TherapySessionTable.update(
                    where = { TherapySessionTable.id eq sessionUuid }
                ) { row ->
                    row[updatedAt] = now
                }

                ResultType.Success(
                    TherapyAssetReplacementResult(
                        therapyAssetId = TherapyAssetId(therapyAssetUuid),
                        replacedStorageKeys = replacedStorageKeys,
                    )
                )
            }
        } catch (exception: Exception) {
            ResultType.Failure(
                DataError.DatabaseError(
                    operation = operation,
                    cause = exception.message,
                )
            )
        }
    }

    override suspend fun getAsset(
        therapySessionId: TherapySessionId,
        therapyAssetId: TherapyAssetId,
    ): ResultType<TherapyAsset, DataError> {
        val operation = "getTherapyAsset"

        return try {
            val asset = db.dbQuery {
                TherapyAssetTable.selectAll()
                    .where {
                        (TherapyAssetTable.id eq therapyAssetId.value) and
                                (TherapyAssetTable.therapySessionId eq therapySessionId.value)
                    }
                    .singleOrNull()
                    ?.toTherapyAsset()
            }

            if (asset == null) {
                ResultType.Failure(DataError.NotFound)
            } else {
                ResultType.Success(asset)
            }
        } catch (exception: Exception) {
            ResultType.Failure(
                DataError.DatabaseError(
                    operation = operation,
                    cause = exception.message,
                )
            )
        }
    }

    private fun ResultRow.belongsToReplacementSlot(
        therapyModuleId: UUID?,
        asset: TherapyAsset,
    ): Boolean {
        val persistedModuleId = this[TherapyAssetTable.therapyModuleId]

        if (persistedModuleId != therapyModuleId) {
            return false
        }

        if (therapyModuleId == null) {
            return true
        }

        return this[TherapyAssetTable.role] == asset.role.name &&
                normalizedLocale(this[TherapyAssetTable.locale]) == normalizedLocale(asset.locale)
    }

    private fun ResultRow.matches(asset: TherapyAsset): Boolean =
        this[TherapyAssetTable.role] == asset.role.name &&
                this[TherapyAssetTable.mediaType] == asset.mediaType.name &&
                this[TherapyAssetTable.storageKey].trim() == asset.storageKey.trim() &&
                this[TherapyAssetTable.mimeType].equals(asset.mimeType.trim(), ignoreCase = true) &&
                this[TherapyAssetTable.sizeBytes] == asset.sizeBytes &&
                this[TherapyAssetTable.sha256].equals(asset.sha256.trim(), ignoreCase = true) &&
                normalizedLocale(this[TherapyAssetTable.locale]) == normalizedLocale(asset.locale) &&
                this[TherapyAssetTable.altText]?.trim() == asset.altText?.trim() &&
                this[TherapyAssetTable.transcript]?.trim() == asset.transcript?.trim()

    private fun ResultRow.toTherapyAsset(): TherapyAsset =
        TherapyAsset(
            id = TherapyAssetId(this[TherapyAssetTable.id].value),
            role = TherapyAssetRole.valueOf(this[TherapyAssetTable.role]),
            mediaType = TherapyMediaType.valueOf(this[TherapyAssetTable.mediaType]),
            storageKey = this[TherapyAssetTable.storageKey],
            mimeType = this[TherapyAssetTable.mimeType],
            sizeBytes = this[TherapyAssetTable.sizeBytes],
            sha256 = this[TherapyAssetTable.sha256],
            locale = this[TherapyAssetTable.locale],
            altText = this[TherapyAssetTable.altText],
            transcript = this[TherapyAssetTable.transcript],
            createdAt = this[TherapyAssetTable.createdAt],
            updatedAt = this[TherapyAssetTable.updatedAt],
        )

    private fun normalizedLocale(locale: String?): String? =
        locale?.trim()?.lowercase(Locale.ROOT)
}
