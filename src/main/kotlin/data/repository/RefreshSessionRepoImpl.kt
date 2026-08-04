package com.simbiri.data.repository

import com.simbiri.data.database.entity.auth.RefreshSessionTable
import com.simbiri.data.database.utils.dbQuery
import com.simbiri.data.mapper.auth.toDomain
import com.simbiri.data.mapper.auth.toEntityForCreate
import com.simbiri.data.mapper.auth.toRefreshSessionEntity
import com.simbiri.data.repository.util.databaseError
import com.simbiri.data.repository.util.validationError
import com.simbiri.domain.model.auth.RefreshSession
import com.simbiri.domain.model.common.RefreshSessionId
import com.simbiri.domain.repository.RefreshSessionRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.Clock
import java.time.Instant

/**
 * PostgreSQL persistence for opaque refresh sessions.
 */
class RefreshSessionRepoImpl(
    private val db: Database,
    private val clock: Clock,
) : RefreshSessionRepository {

    override suspend fun createSession(
        session: RefreshSession,
    ): ResultType<
            RefreshSessionId,
            DataError,
            > {
        val operation = "createRefreshSession"

        if (session.id != null || session.createdAt != null || session.updatedAt != null) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "session.persistenceState",
                    value = "<redacted>",
                    reason = "A new refresh session must not contain an ID or persistence timestamps.",
                )
            )
        }

        return try {
            db.dbQuery {
                val now = Instant.now(clock)
                val entity = session.toEntityForCreate(
                    now
                )

                RefreshSessionTable.insert { row ->
                        row[id] = entity.id
                        row[userId] = entity.userId
                        row[familyId] = entity.familyId
                        row[tokenHash] = entity.tokenHash
                        row[sessionVersion] = entity.sessionVersion
                        row[expiresAt] = entity.expiresAt
                        row[revokedAt] = entity.revokedAt
                        row[createdAt] = entity.createdAt
                        row[updatedAt] = entity.updatedAt
                    }

                ResultType.Success(
                    RefreshSessionId(
                        entity.id
                    )
                )
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                // we avoid including the token hash in the error logged
                    details = "userId=${session.userId.value}, familyId=${session.familyId.value}, " +
                            "sessionVersion=${session.sessionVersion}",
                )
            )
        }
    }

    override suspend fun getSessionByTokenHash(
        tokenHash: String,
    ): ResultType<
            RefreshSession,
            DataError,
            > {
        val operation = "getRefreshSessionByTokenHash"

        if (!tokenHash.matches(
                Regex("^[0-9a-f]{64}$")
            )
        ) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "tokenHash",
                    value = "<redacted>",
                    reason = "Token hash has an invalid format.",
                )
            )
        }

        return try {
            val session = db.dbQuery {
                RefreshSessionTable.selectAll().where {
                        RefreshSessionTable.tokenHash eq tokenHash
                    }.singleOrNull()?.toRefreshSessionEntity()?.toDomain()
            }

            if (session == null) {
                ResultType.Failure(
                    DataError.NotFound
                )
            } else {
                ResultType.Success(
                    session
                )
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "tokenHash=<redacted>",
                )
            )
        }
    }
}
