package com.simbiri.application.user

import com.simbiri.domain.model.user.User
import com.simbiri.domain.policy.user.UserPolicy
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.time.Clock
import java.time.LocalDate

/**
 * Updates an existing user.
 *
 * Responsibilities:
 * - verify that the operation strictly represents an update;
 * - enforce user-domain policies;
 * - delegate persistence to UserRepository.
 *
 * It does not:
 * - parse HTTP path parameters;
 * - handle request or response DTOs.
 */
class UpdateUserUseCase(
    private val userRepository: UserRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(
        user: User,
    ): ResultType<Unit, DataError> {
        if (user.id == null) {
            return ResultType.Failure(
                DataError.ValidationError(
                    message = "User update failed. " +
                            "An existing user ID is required."
                )
            )
        }

        UserPolicy.validateForUpsert(
            user = user,
            today = LocalDate.now(clock),
        )?.let { validationError ->
            return ResultType.Failure(validationError)
        }

        return userRepository.updateUser(user)
    }
}