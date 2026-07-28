package com.simbiri.data.repository

import com.simbiri.data.database.entity.auth.AuthenticationCredentialTable
import com.simbiri.data.database.entity.user.UserTable
import com.simbiri.data.database.utils.dbQuery
import com.simbiri.data.mapper.user.toEntity
import com.simbiri.data.repository.util.databaseError
import com.simbiri.domain.model.auth.UserRegistration
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.repository.UserRegistrationRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import java.time.Clock
import java.time.Instant

/**
 * Persists the profile and authentication credential in one PostgreSQL
 * transaction.
 */
class UserRegistrationRepoImpl(
    private val db: Database,
    private val clock: Clock,
) : UserRegistrationRepository {

    override suspend fun register(
        registration: UserRegistration,
    ): ResultType<UserId, DataError> {
        val operation = "registerUserAccount"

        return try {
            db.dbQuery {
                val now = Instant.now(clock)

                val userEntity = registration.user.toEntity(now)

                /*
                 * These checks provide useful deterministic client errors.
                 * Database unique constraints remain the final authority
                 * when registrations occur concurrently.
                 */
                val accountNameExists = UserTable.selectAll().where {
                        UserTable.accountName eq userEntity.accountName
                    }.limit(1).any()

                if (accountNameExists) {
                    return@dbQuery ResultType.Failure(
                        DataError.DuplicateResource(
                            message = "Registration failed. The account name is unavailable."
                        )
                    )
                }

                val emailAddressExists = UserTable.selectAll().where {
                        UserTable.emailAddress.lowerCase() eq userEntity.emailAddress
                    }.limit(1).any()

                if (emailAddressExists) {
                    return@dbQuery ResultType.Failure(
                        DataError.DuplicateResource(
                            message = "Registration failed. An account already uses this " + "email address."
                        )
                    )
                }


                UserTable.insert { row ->
                    row[UserTable.id] = userEntity.id
                    row[accountName] = userEntity.accountName
                    row[emailAddress] = userEntity.emailAddress
                    row[firstName] = userEntity.firstName
                    row[lastName] = userEntity.lastName
                    row[birthDate] = userEntity.birthDate
                    row[about] = userEntity.about
                    row[tagline] = userEntity.tagline
                    row[profileUrl] = userEntity.profileUrl
                    row[backgroundUrl] = userEntity.backgroundUrl
                    row[userType] = userEntity.userTypeCode
                    row[emailOptIn] = userEntity.emailOptIn
                    row[isPrivate] = userEntity.isPrivate
                    row[isAnonymous] = userEntity.isAnonymous
                    row[isActive] = userEntity.isActive
                    row[createdAt] = userEntity.createdAt
                    row[updatedAt] = userEntity.updatedAt
                }

                AuthenticationCredentialTable.insert { row ->
                        row[userId] = userEntity.id
                        row[passwordHash] = registration.passwordHash
                        row[passwordAlgorithm] = registration.passwordAlgorithm.name
                        row[passwordUpdatedAt] = registration.passwordUpdatedAt
                        row[failedLoginAttempts] = 0
                        row[lockedUntil] = null
                        row[createdAt] = now
                        row[updatedAt] = now
                    }

                ResultType.Success(
                    UserId(userEntity.id)
                )
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "accountName=" + registration.user.accountName + ", userType=" + registration.user.type,
                )
            )
        }
    }
}
