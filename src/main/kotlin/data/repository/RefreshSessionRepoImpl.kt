package com.simbiri.data.repository

import com.simbiri.data.database.entity.auth.AuthenticationCredentialTable
import com.simbiri.data.database.entity.auth.RefreshSessionTable
import com.simbiri.data.database.entity.user.UserTable
import com.simbiri.data.database.utils.dbQuery
import com.simbiri.data.mapper.auth.toDomain
import com.simbiri.data.mapper.auth.toEntityForCreate
import com.simbiri.data.mapper.auth.toRefreshSessionEntity
import com.simbiri.data.repository.util.databaseError
import com.simbiri.data.repository.util.validationError
import com.simbiri.domain.model.auth.RefreshSession
import com.simbiri.domain.model.auth.RefreshSessionRotationResult
import com.simbiri.domain.model.common.RefreshSessionId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.repository.RefreshSessionRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import java.time.Clock
import java.time.Instant
import java.util.*

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
                    details = "userId=${session.userId.value}, familyId=${session.familyId.value}, "
                            + "sessionVersion=${session.sessionVersion}",
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

    override suspend fun rotateSession(
        presentedTokenHash: String,
        replacementTokenHash: String,
        replacementExpiresAt: Timestamp,
    ): ResultType<
            RefreshSessionRotationResult,
            DataError,
            > {
        val operation = "rotateRefreshSession"

        if (!isSha256Hash(presentedTokenHash) || !isSha256Hash(replacementTokenHash)) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "tokenHash",
                    value = "<redacted>",
                    reason = "Refresh-token hashes must be lowercase SHA-256 digests.",
                )
            )
        }

        if (presentedTokenHash == replacementTokenHash) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "replacementTokenHash",
                    value = "<redacted>",
                    reason = "Replacement token must differ from the presented token.",
                )
            )
        }

        return try {
            db.dbQuery {
                val now = Instant.now(clock)

                if (!replacementExpiresAt.isAfter(now)) {
                    return@dbQuery ResultType.Failure(
                        validationError(
                            operation = operation,
                            field = "replacementExpiresAt",
                            value = replacementExpiresAt.toString(),
                            reason = "Replacement refresh session must expire in the future.",
                        )
                    )
                }

                /*
                 * PostgreSQL locks this row until the transaction completes.
                 * Two concurrent requests therefore cannot both consume the
                 * same refresh token successfully.
                 */
                val currentRow = RefreshSessionTable.selectAll().where {
                    RefreshSessionTable.tokenHash eq presentedTokenHash
                }.forUpdate().singleOrNull() ?: return@dbQuery ResultType.Success(
                    RefreshSessionRotationResult.Invalid
                )

                val currentSessionId = currentRow[RefreshSessionTable.id]
                val familyId = currentRow[RefreshSessionTable.familyId]
                val persistedUserId = currentRow[RefreshSessionTable.userId]
                val currentSessionVersion = currentRow[RefreshSessionTable.sessionVersion]
                val currentRevokedAt = currentRow[RefreshSessionTable.revokedAt]
                val currentExpiresAt = currentRow[RefreshSessionTable.expiresAt]

                if (currentRevokedAt != null) {
                    /*
                     * Reuse of a consumed token should be flagged as theft. Revoke every
                     * active token in the same rotation family.
                     */
                    revokeFamilyInternal(
                        familyId = familyId,
                        revokedAt = now,
                    )

                    return@dbQuery ResultType.Success(
                        RefreshSessionRotationResult.ReuseDetected
                    )
                }

                if (!currentExpiresAt.isAfter(now)) {
                    revokeFamilyInternal(
                        familyId = familyId,
                        revokedAt = now,
                    )

                    return@dbQuery ResultType.Success(
                        RefreshSessionRotationResult.Invalid
                    )
                }

                val authenticationRow = (UserTable innerJoin AuthenticationCredentialTable).selectAll().where {
                    UserTable.id eq EntityID(
                        id = persistedUserId,

                        table = UserTable,
                    )
                }.singleOrNull()

                val sessionIsCurrent =
                    authenticationRow != null && authenticationRow[UserTable.isActive] &&
                            authenticationRow[AuthenticationCredentialTable.sessionVersion] == currentSessionVersion

                if (!sessionIsCurrent) {
                    /*
                     * Password changes, logout-all-devices, account
                     * deactivation, or missing credentials make the entire
                     * refresh family unusable.
                     */
                    revokeFamilyInternal(
                        familyId = familyId,
                        revokedAt = now,
                    )

                    return@dbQuery ResultType.Success(
                        RefreshSessionRotationResult.Invalid
                    )
                }

                val revokedRows = RefreshSessionTable.update(
                    where = {
                        (RefreshSessionTable.id eq currentSessionId) and RefreshSessionTable.revokedAt.isNull()
                    }) { row ->
                    row[revokedAt] = now
                    row[updatedAt] = now
                }

                if (revokedRows != 1) {
                    return@dbQuery ResultType.Failure(
                        DataError.UnknownError(
                            cause = "Current refresh session could not " + "be consumed."
                        )
                    )
                }

                val replacementSessionId = UUID.randomUUID()

                RefreshSessionTable.insert { row ->
                    row[id] = replacementSessionId
                    row[userId] = persistedUserId
                    row[RefreshSessionTable.familyId] = familyId
                    row[tokenHash] = replacementTokenHash
                    row[sessionVersion] = currentSessionVersion
                    row[expiresAt] = replacementExpiresAt
                    row[revokedAt] = null
                    row[createdAt] = now
                    row[updatedAt] = now
                }

                ResultType.Success(
                    RefreshSessionRotationResult.Rotated(
                        refreshSessionId = RefreshSessionId(
                            replacementSessionId
                        ),

                        userId = UserId(
                            persistedUserId
                        ),

                        sessionVersion = currentSessionVersion,
                    )
                )
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "presentedTokenHash=<redacted>, replacementTokenHash=<redacted>",
                )
            )
        }
    }

    /**
     * Revokes every currently active token in a rotation family.
     *
     * This must execute inside an active Exposed transaction.
     */
    private fun revokeFamilyInternal(
        familyId: UUID,
        revokedAt: Instant,
    ) {
        RefreshSessionTable.update(
            where = {
                (RefreshSessionTable.familyId eq familyId) and RefreshSessionTable.revokedAt.isNull()
            }) { row ->
            row[RefreshSessionTable.revokedAt] = revokedAt
            row[RefreshSessionTable.updatedAt] = revokedAt
        }
    }

    private fun isSha256Hash(
        value: String,
    ): Boolean = value.matches(
        Regex("^[0-9a-f]{64}$")
    )
}
