package com.simbiri.data.repository

import com.simbiri.data.database.entity.auth.AuthenticationCredentialTable
import com.simbiri.data.database.entity.user.UserTable
import com.simbiri.data.database.utils.dbQuery
import com.simbiri.data.mapper.auth.toAuthenticationCredentialEntity
import com.simbiri.data.mapper.auth.toDomain
import com.simbiri.data.mapper.auth.toEntityForCreate
import com.simbiri.data.repository.util.databaseError
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

class AuthenticationCredentialRepoImpl(
    private val db: Database,
    private val clock: Clock = Clock.systemUTC(),
) : AuthenticationCredentialRepository {

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
                    value = "createdAt=${credential.createdAt}, " + "updatedAt=${credential.updatedAt}",
                    reason = "A new credential must not already " + "contain persistence timestamps."
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
                        DataError.ForeignKeyViolation(
                            message = "$operation failed. No persisted User exists for " +
                                    "userId=${credential.userId.value}."
                        )
                    )
                }

                val credentialAlreadyExists = AuthenticationCredentialTable.selectAll().where {
                        AuthenticationCredentialTable.userId eq credential.userId.value
                    }.limit(1).any()

                if (credentialAlreadyExists) {
                    return@dbQuery ResultType.Failure(
                        DataError.DuplicateResource(
                            message = "$operation failed. A credential already exists for " +
                                    "userId=${credential.userId.value}."
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

    override suspend fun updateCredential(
        credential: AuthenticationCredential,
    ): ResultType<Unit, DataError> {
        val operation = "updateCredential"

        return try {
            db.dbQuery {
                val now = Instant.now(clock)

                val updatedRows = AuthenticationCredentialTable.update(
                        where = {
                            AuthenticationCredentialTable.userId eq credential.userId.value
                        }) { row ->
                        row[passwordHash] = credential.passwordHash
                        row[passwordAlgorithm] = credential.passwordAlgorithm.name
                        row[passwordUpdatedAt] = credential.passwordUpdatedAt
                        row[failedLoginAttempts] = credential.failedLoginAttempts
                        row[lockedUntil] = credential.lockedUntil
                        row[updatedAt] = now
                    }

                if (updatedRows == 0) {
                    ResultType.Failure(
                        DataError.NotFound
                    )
                } else {
                    ResultType.Success(Unit)
                }
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "userId=${credential.userId.value}, " + "algorithm=" + credential.passwordAlgorithm,
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
}
