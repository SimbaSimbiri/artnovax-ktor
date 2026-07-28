package com.simbiri.application.user

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.user.User
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Retrieves the complete profile belonging to the authenticated user.
 */
class GetCurrentUserUseCase(
    private val userRepository: UserRepository,
) {

    suspend operator fun invoke(
        authenticatedUserId: UserId,
    ): ResultType<User, DataError> = when (val result = userRepository.getUserById(
        authenticatedUserId
    )) {
        is ResultType.Success -> {
            if (!result.data.isActive) {
                ResultType.Failure(
                    DataError.Forbidden(
                        message = "Current-user profile access failed. The authenticated account is inactive."
                    )
                )
            } else {
                result
            }
        }

        is ResultType.Failure -> ResultType.Failure(
            result.error
        )
    }
}
