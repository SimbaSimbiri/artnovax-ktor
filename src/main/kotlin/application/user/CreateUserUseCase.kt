package com.simbiri.application.user

import com.simbiri.domain.model.user.User
import com.simbiri.domain.policy.user.UserPolicy
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.time.Clock
import java.time.LocalDate

/**
 * Creates a new user
 *
 * Responsibilities:
 * - verifies we are strictly creating, not updating
 * - enforces user-domain policies
 * - delegates persistence to UserRepository
 *
 * Does not:
 * - handle HTTP requests/responses
 * - validate DTO formatting
 */

class CreateUserUseCase(private val userRepository: UserRepository, private val clock: Clock) {

    suspend operator fun invoke(user: User): ResultType<Unit, DataError> {
        if (user.id != null) {
            return ResultType.Failure(
                DataError.ValidationError(
                    message = "User creation failed. " +
                            "A new user must not already have an ID. " +
                            "receivedUserId=${user.id}."
                )
            )
        }

        UserPolicy.validateForUpsert(user = user, today = LocalDate.now(clock))?.let { validationError ->
            return ResultType.Failure(validationError)
        }

        /*
         * Transitional call:
         *
         * We will replace this with createUser() when we refactor
         * UserRepository and UserRepoImpl.
         */
        return userRepository.createUser(user)
    }
}