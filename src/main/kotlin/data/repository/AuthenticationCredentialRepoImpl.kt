package com.simbiri.data.repository

import com.simbiri.data.database.entity.auth.AuthenticationCredentialTable
import com.simbiri.data.database.entity.user.UserTable
import com.simbiri.data.database.utils.dbQuery
import com.simbiri.data.mapper.auth.toAuthenticationCredentialEntity
import com.simbiri.data.mapper.auth.toDomain
import com.simbiri.data.mapper.auth.toEntityForCreate
import com.simbiri.data.repository.util.databaseError
import com.simbiri.data.repository.util.duplicateResourceError
import com.simbiri.data.repository.util.foreignKeyError
import com.simbiri.data.repository.util.validationError
import com.simbiri.domain.model.auth.AuthenticationCredential
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.repository.AuthenticationCredentialRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.time.Instant
import com.simbiri.domain.model.auth.AuthenticationAttemptMutationResult
import com.simbiri.domain.model.auth.PasswordHashAlgorithm
import com.simbiri.domain.policy.auth.AuthenticationAttemptPolicy
import com.simbiri.domain.repository.AuthenticationCredentialMutationRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.and

class AuthenticationCredentialRepoImpl(
    private val db: Database,
    private val clock: Clock,
) : AuthenticationCredentialRepository, AuthenticationCredentialMutationRepository {

    override suspend fun getCredentialByUserId(
        userId: UserId,
    ): ResultType<AuthenticationCredential, DataError> {
        val operation = "getCredentialByUserId"

        return try {
            val credential = db.dbQuery {
                AuthenticationCredentialTable.selectAll().where {
                    AuthenticationCredentialTable.userId eq userId.value
                }.singleOrNull()?.toAuthenticationCredentialEntity()?.toDomain()
            }

            if (credential == null) {
                ResultType.Failure(
                    DataError.NotFound
                )
            } else {
                ResultType.Success(
                    credential
                )
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

    override suspend fun createCredential(
        credential: AuthenticationCredential,
    ): ResultType<Unit, DataError> {
        val operation = "createCredential"

        /*
         * A create operation receives an unpersisted aggregate.
         */
        if (credential.createdAt != null || credential.updatedAt != null) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "credential.persistenceTimestamps",
                    value = "createdAt=${credential.createdAt}, updatedAt=${credential.updatedAt}",
                    reason = "A new credential must not already contain persistence timestamps."
                )
            )
        }

        return try {
            db.dbQuery {
                val persistedUserExists = UserTable.selectAll().where {
                    UserTable.id eq EntityID(
                        id = credential.userId.value,
                        table = UserTable,
                    )
                }.limit(1).any()

                if (!persistedUserExists) {
                    return@dbQuery ResultType.Failure(
                        foreignKeyError(
                            operation = operation,
                            message = "No persisted User exists for userId=${credential.userId.value}."
                        )
                    )
                }

                val credentialAlreadyExists = AuthenticationCredentialTable.selectAll().where {
                    AuthenticationCredentialTable.userId eq credential.userId.value
                }.limit(1).any()

                if (credentialAlreadyExists) {
                    return@dbQuery ResultType.Failure(
                        duplicateResourceError(
                            operation = operation,
                            message = "A credential already exists for " + "userId=${credential.userId.value}."
                        )
                    )
                }

                val now = Instant.now(clock)

                val entity = credential.toEntityForCreate(
                    now = now
                )

                AuthenticationCredentialTable.insert { row ->
                    row[userId] = entity.userId
                    row[passwordHash] = entity.passwordHash
                    row[passwordAlgorithm] = entity.passwordAlgorithm
                    row[passwordUpdatedAt] = entity.passwordUpdatedAt
                    row[failedLoginAttempts] = entity.failedLoginAttempts
                    row[lockedUntil] = entity.lockedUntil
                    row[sessionVersion] = entity.sessionVersion
                    row[createdAt] = entity.createdAt
                    row[updatedAt] = entity.updatedAt
                }

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {/*
             * The foreign-key and uniqueness constraints remain the final
             * authority if another transaction changes state after the
             * pre-insert checks.
             */
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "userId=${credential.userId.value}, algorithm=" + credential.passwordAlgorithm,
                )
            )
        }
    }


    override suspend fun recordFailedLoginAttempt(
        userId: UserId,
        expectedPasswordHash: String,
        expectedSessionVersion: Long,
        attemptedAt: Instant,
    ): ResultType<
            AuthenticationAttemptMutationResult,
            DataError,
            > {
        val operation = "recordFailedLoginAttempt"

        if (expectedPasswordHash.isBlank() || expectedSessionVersion <= 0L) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "expectedCredential",
                    value = "<redacted>",
                    reason = "Expected password hash and session version " + "must be valid.",
                )
            )
        }

        return try {
            db.dbQuery {
                val credential = loadCredentialForUpdate(
                    userId
                ) ?: return@dbQuery ResultType.Failure(
                    DataError.NotFound
                )

                if (!credential.matchesSnapshot(
                        expectedPasswordHash = expectedPasswordHash,
                        expectedSessionVersion = expectedSessionVersion,
                    )
                ) {
                    return@dbQuery ResultType.Success(
                        AuthenticationAttemptMutationResult.StaleCredential
                    )
                }

                if (credential.isLockedAt(
                        attemptedAt
                    )
                ) {
                    return@dbQuery ResultType.Success(
                        AuthenticationAttemptMutationResult.TemporarilyLocked
                    )
                }

                val updatedCredential = AuthenticationAttemptPolicy.afterFailedAttempt(
                        credential = credential,

                        attemptedAt = attemptedAt,
                    )

                val updatedRows = updateAttemptStateInternal(
                    credential = updatedCredential,

                    updatedAt = Instant.now(clock),
                )

                if (updatedRows != 1) {
                    return@dbQuery ResultType.Failure(
                        DataError.UnknownError(
                            cause = "Failed-login state could not be updated."
                        )
                    )
                }

                ResultType.Success(
                    AuthenticationAttemptMutationResult.Applied(
                            sessionVersion = credential.sessionVersion
                        )
                )
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "userId=${userId.value}, " + "expectedSessionVersion=" + expectedSessionVersion,
                )
            )
        }
    }

    override suspend fun recordSuccessfulLogin(
        userId: UserId,
        expectedPasswordHash: String,
        expectedSessionVersion: Long,
        authenticatedAt: Instant,
    ): ResultType<
            AuthenticationAttemptMutationResult,
            DataError,
            > {
        val operation = "recordSuccessfulLogin"

        if (expectedPasswordHash.isBlank() || expectedSessionVersion <= 0L) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "expectedCredential",
                    value = "<redacted>",
                    reason = "Expected password hash and session version " + "must be valid.",
                )
            )
        }

        return try {
            db.dbQuery {
                val credential = loadCredentialForUpdate(
                    userId
                ) ?: return@dbQuery ResultType.Failure(
                    DataError.NotFound
                )

                if (!credential.matchesSnapshot(
                        expectedPasswordHash = expectedPasswordHash,
                        expectedSessionVersion = expectedSessionVersion,
                    )
                ) {
                    return@dbQuery ResultType.Success(
                        AuthenticationAttemptMutationResult.StaleCredential
                    )
                }

                /*
                 * Recheck lock state under the row lock. Another request may
                 * have locked the credential after the caller initially read it.
                 */
                if (credential.isLockedAt(
                        authenticatedAt
                    )
                ) {
                    return@dbQuery ResultType.Success(
                        AuthenticationAttemptMutationResult.TemporarilyLocked
                    )
                }

                val updatedCredential = AuthenticationAttemptPolicy.afterSuccessfulAttempt(
                        credential
                    )

                if (updatedCredential != credential) {
                    val updatedRows = updateAttemptStateInternal(
                        credential = updatedCredential,
                        updatedAt = Instant.now(clock),
                    )

                    if (updatedRows != 1) {
                        return@dbQuery ResultType.Failure(
                            DataError.UnknownError(
                                cause = "Successful-login state could not " + "be updated."
                            )
                        )
                    }
                }

                ResultType.Success(
                    AuthenticationAttemptMutationResult.Applied(
                            sessionVersion = credential.sessionVersion
                        )
                )
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "userId=${userId.value}, " + "expectedSessionVersion=" + expectedSessionVersion,
                )
            )
        }
    }

    override suspend fun replacePasswordAndIncrementSessionVersion(
        userId: UserId,
        expectedPasswordHash: String,
        expectedSessionVersion: Long,
        passwordHash: String,
        passwordAlgorithm: PasswordHashAlgorithm,
        passwordUpdatedAt: Instant,
    ): ResultType<Long, DataError> {
        val operation = "replacePasswordAndIncrementSessionVersion"

        if (expectedPasswordHash.isBlank() || passwordHash.isBlank()) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "passwordHash",
                    value = "<redacted>",
                    reason = "Password hashes must not be blank.",
                )
            )
        }

        if (expectedSessionVersion <= 0L) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "expectedSessionVersion",
                    value = expectedSessionVersion.toString(),
                    reason = "Expected session version must be positive.",
                )
            )
        }

        if (expectedSessionVersion == Long.MAX_VALUE) {
            return ResultType.Failure(
                DataError.UnknownError(
                    cause = "Credential session version cannot be incremented."
                )
            )
        }

        return try {
            db.dbQuery {
                val now = Instant.now(clock)

                /*
                 * The password replacement and version increment execute in one
                 * SQL statement. Failed-login requests cannot overwrite either
                 * value because they now update only attempt-state columns.
                 */
                val updatedRows = AuthenticationCredentialTable.update(
                    where = {
                        (AuthenticationCredentialTable.userId eq userId.value) and
                                (AuthenticationCredentialTable.passwordHash eq expectedPasswordHash) and
                                (AuthenticationCredentialTable.sessionVersion eq expectedSessionVersion) and
                                (AuthenticationCredentialTable.sessionVersion less Long.MAX_VALUE)
                    }) { row ->
                    row[AuthenticationCredentialTable.passwordHash] = passwordHash
                    row[AuthenticationCredentialTable.passwordAlgorithm] = passwordAlgorithm.name
                    row[AuthenticationCredentialTable.passwordUpdatedAt] = passwordUpdatedAt
                    row[AuthenticationCredentialTable.failedLoginAttempts] = 0
                    row[AuthenticationCredentialTable.lockedUntil] = null
                    row[AuthenticationCredentialTable.sessionVersion] = AuthenticationCredentialTable.sessionVersion + 1L
                    row[AuthenticationCredentialTable.updatedAt] = now
                }

                if (updatedRows == 1) {
                    return@dbQuery ResultType.Success(
                        expectedSessionVersion + 1L
                    )
                }

                val persistedCredential = AuthenticationCredentialTable.selectAll().where {
                        AuthenticationCredentialTable.userId eq userId.value
                    }.singleOrNull()

                if (persistedCredential == null) {
                    ResultType.Failure(
                        DataError.NotFound
                    )
                } else {/*
                     * A password change, logout-all-devices operation, or another
                     * concurrent credential mutation changed the expected
                     * snapshot.
                     */
                    ResultType.Failure(
                        DataError.Conflict(
                            message = "Authentication credential changed " + "concurrently."
                        )
                    )
                }
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "userId=${userId.value}, " + "expectedSessionVersion=" + expectedSessionVersion,
                )
            )
        }
    }

    override suspend fun deleteCredentialByUserId(
        userId: UserId,
    ): ResultType<Unit, DataError> {
        val operation = "deleteCredentialByUserId"

        return try {
            val deletedRows = db.dbQuery {
                AuthenticationCredentialTable.deleteWhere {
                    AuthenticationCredentialTable.userId eq userId.value
                }
            }

            if (deletedRows == 0) {
                ResultType.Failure(
                    DataError.NotFound
                )
            } else {
                ResultType.Success(Unit)
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

    /**
     * Loads and locks one credential for an attempt-state mutation.
     */
    private fun loadCredentialForUpdate(
        userId: UserId,
    ): AuthenticationCredential? = AuthenticationCredentialTable.selectAll().where {
            AuthenticationCredentialTable.userId eq userId.value
        }.forUpdate().singleOrNull()?.toAuthenticationCredentialEntity()?.toDomain()

    /**
     * Updates only failed-attempt fields.
     *
     * Password state and sessionVersion are intentionally excluded.
     */
    private fun updateAttemptStateInternal(
        credential: AuthenticationCredential,
        updatedAt: Instant,
    ): Int = AuthenticationCredentialTable.update(
        where = {
            AuthenticationCredentialTable.userId eq credential.userId.value
        }) { row ->
        row[AuthenticationCredentialTable.failedLoginAttempts] = credential.failedLoginAttempts
        row[AuthenticationCredentialTable.lockedUntil] = credential.lockedUntil
        row[AuthenticationCredentialTable.updatedAt] = updatedAt
    }

    /**
     * Confirms that password verification was performed against the credential
     * currently locked by the transaction.
     */
    private fun AuthenticationCredential.matchesSnapshot(
        expectedPasswordHash: String,
        expectedSessionVersion: Long,
    ): Boolean = passwordHash == expectedPasswordHash && sessionVersion == expectedSessionVersion
}
