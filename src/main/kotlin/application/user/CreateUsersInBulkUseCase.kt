package com.simbiri.application.user

import com.simbiri.domain.model.user.User
import com.simbiri.domain.policy.user.UserPolicy
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.time.Clock
import java.time.LocalDate

/**
 * Creates multiple users in one operation.
 *
 * Responsibilities:
 * - ensure every item represents a new user;
 * - validate every user before any database write begins;
 * - add the failing item's index to validation errors;
 * - delegate transactional persistence to UserRepository.
 *
 */
class CreateUsersInBulkUseCase(
    private val userRepository: UserRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(
        users: List<User>,
    ): ResultType<Unit, DataError> {
        if (users.isEmpty()) {
            return ResultType.Success(Unit)
        }

        val today = LocalDate.now(clock)

        users.forEachIndexed { index, user ->
            if (user.id != null) {
                return ResultType.Failure(
                    DataError.ValidationError(
                        message = "Bulk user creation failed. " +
                                "users[$index] must not already have an ID. " +
                                "receivedUserId=${user.id}, " +
                                "accountName=${user.accountName}."
                    )
                )
            }

            UserPolicy.validateForUpsert(
                user = user,
                today = today,
            )?.let { validationError ->
                return ResultType.Failure(
                    validationError.withBulkContext(
                        index = index,
                        user = user,
                    )
                )
            }
        }

        return userRepository.createUsers(users)
    }

    private fun DataError.ValidationError.withBulkContext(
        index: Int,
        user: User,
    ): DataError.ValidationError =
        DataError.ValidationError(
            message = "Bulk user creation failed at users[$index]. " +
                    "accountName=${user.accountName}, " +
                    "emailAddress=${user.emailAddress}. " +
                    "Validation details: $message"
        )
}