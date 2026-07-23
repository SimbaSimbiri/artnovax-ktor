package com.simbiri.data.repository

import com.simbiri.data.database.entity.therapy.*
import com.simbiri.data.mapper.therapy.toDomain
import com.simbiri.data.mapper.therapy.toTherapyAssetEntity
import com.simbiri.data.mapper.therapy.toTherapyModuleEntity
import com.simbiri.data.mapper.therapy.toTherapySessionEntity
import com.simbiri.domain.model.therapy.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import java.util.*

/**
 * Reconstructs complete TherapySession aggregates from normalized
 * persistence tables. Performs batched reconstruction instead of
 * issuing separate queries per session
 *
 * Every function here must be called from an active Exposed
 * transaction, through a dbQuery.
 *
 * This reader:
 * - loads session rows;
 * - loads modules in batches;
 * - loads assets in batches;
 * - rebuilds module-owned assets;
 * - rebuilds the session cover asset;
 * - loads goals, contraindications, and culture tags;
 * - returns modules in orderIndex order.
 */
internal object TherapyContentAggregateReader {

    fun loadById(
        therapySessionId: UUID,
    ): TherapySession? {
        val entity = TherapySessionTable.selectAll().where {
                TherapySessionTable.id eq therapySessionId
            }.singleOrNull()?.toTherapySessionEntity() ?: return null

        return hydrate(
            entities = listOf(entity),
        ).single()
    }

    fun loadLatestBySeriesId(
        seriesId: UUID,
    ): TherapySession? {
        val entity = TherapySessionTable.selectAll().where {
                TherapySessionTable.seriesId eq seriesId
            }.orderBy(
                TherapySessionTable.version to SortOrder.DESC
            ).limit(1).singleOrNull()?.toTherapySessionEntity() ?: return null

        return hydrate(
            entities = listOf(entity),
        ).single()
    }

    fun loadMany(
        status: TherapyContentStatus?,
        authorId: UUID?,
        goal: TherapyGoal?,
        intensity: TherapyIntensity?,
        locale: String?,
    ): List<TherapySession> {
        var query: Query = TherapySessionTable.selectAll()

        if (status != null) {
            query = query.andWhere {
                TherapySessionTable.status eq status.name
            }
        }

        if (authorId != null) {
            query = query.andWhere {
                TherapySessionTable.authorId eq authorId
            }
        }

        if (intensity != null) {
            query = query.andWhere {
                TherapySessionTable.intensity eq intensity.name
            }
        }

        if (locale != null) {
            query = query.andWhere {
                TherapySessionTable.locale eq locale.trim()
            }
        }

        if (goal != null) {
            val matchingSessionIds = TherapySessionGoalTable.selectAll().where {
                    TherapySessionGoalTable.goal eq goal.name
                }.map { row ->
                    row[TherapySessionGoalTable.therapySessionId]
                }.distinct()

            if (matchingSessionIds.isEmpty()) {
                return emptyList()
            }

            val matchingEntityIds = matchingSessionIds.map { sessionId ->
                EntityID(
                    id = sessionId,
                    table = TherapySessionTable,
                )
            }

            query = query.andWhere {
                TherapySessionTable.id inList matchingEntityIds
            }
        }

        val entities = query.orderBy(
                TherapySessionTable.updatedAt to SortOrder.DESC
            ).map(ResultRow::toTherapySessionEntity)

        return hydrate(entities)
    }

    private fun hydrate(
        entities: List<TherapySessionEntity>,
    ): List<TherapySession> {
        if (entities.isEmpty()) {
            return emptyList()
        }

        val sessionIds = entities.map { entity -> entity.id }.toSet()

        val moduleEntities = loadModuleEntities(
            therapySessionIds = sessionIds,
        )

        val assetEntities = loadAssetEntities(
            therapySessionIds = sessionIds,
        )

        val moduleAssetsByModuleId = mapModuleAssets(
            assetEntities = assetEntities,
        )

        val coverAssetsBySessionId = mapCoverAssets(
            assetEntities = assetEntities,
        )

        val modulesBySessionId = mapModules(
            moduleEntities = moduleEntities,
            moduleAssetsByModuleId = moduleAssetsByModuleId,
        )

        val goalsBySessionId = loadGoals(
            therapySessionIds = sessionIds,
        )

        val contraindicationsBySessionId = loadContraindications(
            therapySessionIds = sessionIds,
        )

        val cultureTagsBySessionId = loadCultureTags(
            therapySessionIds = sessionIds,
        )

        return entities.map { entity ->
            entity.toDomain(
                modules = modulesBySessionId[entity.id].orEmpty(),

                goalTags = goalsBySessionId[entity.id].orEmpty(),

                contraindications = contraindicationsBySessionId[entity.id].orEmpty(),

                cultureTags = cultureTagsBySessionId[entity.id].orEmpty(),

                coverAsset = coverAssetsBySessionId[entity.id],
            )
        }
    }

    private fun loadModuleEntities(
        therapySessionIds: Set<UUID>,
    ): List<TherapyModuleEntity> = TherapyModuleTable.selectAll().where {
            TherapyModuleTable.therapySessionId inList therapySessionIds.toList()
        }.orderBy(
            TherapyModuleTable.therapySessionId to SortOrder.ASC,
            TherapyModuleTable.orderIndex to SortOrder.ASC,
        ).map(ResultRow::toTherapyModuleEntity)

    private fun loadAssetEntities(
        therapySessionIds: Set<UUID>,
    ): List<TherapyAssetEntity> = TherapyAssetTable.selectAll().where {
            TherapyAssetTable.therapySessionId inList therapySessionIds.toList()
        }.map(ResultRow::toTherapyAssetEntity)

    private fun mapModuleAssets(
        assetEntities: List<TherapyAssetEntity>,
    ): Map<UUID, List<TherapyAsset>> = assetEntities.filter { entity ->
            entity.therapyModuleId != null
        }.groupBy { entity ->
            requireNotNull(entity.therapyModuleId)
        }.mapValues { (_, entities) ->
            entities.map(TherapyAssetEntity::toDomain).sortedWith(
                    compareBy<TherapyAsset>(
                        { asset -> asset.role.name },
                        { asset -> asset.locale.orEmpty() },
                        { asset -> asset.storageKey },
                    )
                )
        }

    private fun mapCoverAssets(
        assetEntities: List<TherapyAssetEntity>,
    ): Map<UUID, TherapyAsset> {
        val sessionLevelAssets = assetEntities.filter { entity ->
            entity.therapyModuleId == null
        }

        val invalidSessionLevelAsset = sessionLevelAssets.firstOrNull { entity ->
            entity.roleName != TherapyAssetRole.SESSION_COVER.name
        }

        check(invalidSessionLevelAsset == null) {
            "Invalid session-level therapy asset detected. " + "assetId=${invalidSessionLevelAsset?.id}, " + "therapySessionId=" + "${invalidSessionLevelAsset?.therapySessionId}, " + "role=${invalidSessionLevelAsset?.roleName}. " + "Only SESSION_COVER assets may have a null " + "therapyModuleId."
        }

        return sessionLevelAssets.groupBy { entity ->
                entity.therapySessionId
            }.mapValues { (therapySessionId, entities) ->
                check(entities.size == 1) {
                    "Therapy session contains multiple cover assets. " + "therapySessionId=$therapySessionId, " + "coverAssetCount=${entities.size}, " + "assetIds=${entities.map { entity -> entity.id }}."
                }

                entities.single().toDomain()
            }
    }

    private fun mapModules(
        moduleEntities: List<TherapyModuleEntity>,
        moduleAssetsByModuleId: Map<UUID, List<TherapyAsset>>,
    ): Map<UUID, List<TherapyModule>> = moduleEntities.groupBy { entity ->
            entity.therapySessionId
        }.mapValues { (_, entities) ->
            entities.sortedBy { entity ->
                    entity.orderIndex
                }.map { entity ->
                    entity.toDomain(
                        assets = moduleAssetsByModuleId[entity.id].orEmpty()
                    )
                }
        }

    private fun loadGoals(
        therapySessionIds: Set<UUID>,
    ): Map<UUID, Set<TherapyGoal>> = TherapySessionGoalTable.selectAll().where {
            TherapySessionGoalTable.therapySessionId inList therapySessionIds.toList()
        }.groupBy { row ->
            row[TherapySessionGoalTable.therapySessionId]
        }.mapValues { (_, rows) ->
            rows.map { row ->
                    TherapyGoal.valueOf(
                        row[TherapySessionGoalTable.goal]
                    )
                }.toSet()
        }

    private fun loadContraindications(
        therapySessionIds: Set<UUID>,
    ): Map<UUID, Set<TherapyContraindication>> = TherapySessionContraindicationTable.selectAll().where {
            TherapySessionContraindicationTable.therapySessionId inList therapySessionIds.toList()
        }.groupBy { row ->
            row[TherapySessionContraindicationTable.therapySessionId]
        }.mapValues { (_, rows) ->
            rows.map { row ->
                    TherapyContraindication.valueOf(
                        row[TherapySessionContraindicationTable.contraindication]
                    )
                }.toSet()
        }

    private fun loadCultureTags(
        therapySessionIds: Set<UUID>,
    ): Map<UUID, Set<String>> = TherapySessionCultureTagTable.selectAll().where {
            TherapySessionCultureTagTable.therapySessionId inList therapySessionIds.toList()
        }.groupBy { row ->
            row[TherapySessionCultureTagTable.therapySessionId]
        }.mapValues { (_, rows) ->
            rows.map { row ->
                    row[TherapySessionCultureTagTable.tag]
                }.toSet()
        }
}
