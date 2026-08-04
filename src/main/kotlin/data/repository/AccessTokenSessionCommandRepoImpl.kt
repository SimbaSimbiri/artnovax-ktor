package com.simbiri.data.repository

import com.simbiri.data.database.entity.auth.AuthenticationCredentialTable
import com.simbiri.data.database.utils.dbQuery
import com.simbiri.data.repository.util.databaseError
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.repository.AccessTokenSessionCommandRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.time.Instant

/**
 * Atomically invalidates access tokens by incrementing the credential's
 * persisted session version.
 */
class AccessTokenSessionCommandRepoImpl(
    private val db: Database,
    private val clock: Clock,
) : AccessTokenSessionCommandRepository {

    override suspend fun invalidateAllSessions(
        userId: UserId,
    ): ResultType<Unit, DataError> {
        val operation = "invalidateAllAccessTokenSessions"

        return try {
            db.dbQuery {
                val now = Instant.now(clock)

                /*
                 * The increment is executed by PostgreSQL rather than
                 * a load-copy-update sequence. Concurrent requests therefore
                 * cannot overwrite each other's version changes.
                 */
                val updatedRows = AuthenticationCredentialTable.update(
                        where = {
                            (AuthenticationCredentialTable.userId eq userId.value) and
                                    (AuthenticationCredentialTable.sessionVersion less Long.MAX_VALUE)
                        }) { row ->
                        row[AuthenticationCredentialTable.sessionVersion] =
                            AuthenticationCredentialTable.sessionVersion + 1L

                        row[AuthenticationCredentialTable.updatedAt] = now
                    }

                if (updatedRows > 0) {
                    return@dbQuery ResultType.Success(
                        Unit
                    )
                }

                /*
                 * No row updated because either the credential does not
                 * exist or its version can no longer be incremented.
                 */
                val persistedVersion = AuthenticationCredentialTable.selectAll().where {
                        AuthenticationCredentialTable.userId eq userId.value
                    }.singleOrNull()?.get(
                        AuthenticationCredentialTable.sessionVersion
                    )

                when (persistedVersion) {
                    null -> {
                        ResultType.Failure(
                            DataError.NotFound
                        )
                    }
                    Long.MAX_VALUE -> {
                        ResultType.Failure(
                            DataError.UnknownError(
                                cause = "Access-token session version cannot be incremented."
                            )
                        )
                    }
                    else -> {
                        ResultType.Failure(
                            DataError.UnknownError(
                                cause = "Access-token sessions could not be invalidated."
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "userId=${userId.value}",
                )
            )
        }
    }
}
