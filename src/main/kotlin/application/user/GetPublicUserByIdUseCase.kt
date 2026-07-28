package com.simbiri.application.user

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.user.User
import com.simbiri.domain.policy.user.UserProfileVisibilityPolicy
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Retrieves one publicly visible user profile.
 *
 * Private and inactive users are represented as NotFound so public callers
 * cannot determine whether a hidden account exists.
 */
class GetPublicUserByIdUseCase(
    private val userRepository: UserRepository,
) {

    suspend operator fun invoke(
        userId: UserId,
    ): ResultType<User, DataError> = when (val result = userRepository.getUserById(userId)) {
        is ResultType.Success -> if (UserProfileVisibilityPolicy.isPubliclyVisible(
                    result.data
                )
        ) {
            result
        } else {
            ResultType.Failure(
                DataError.NotFound
            )
        }

        is ResultType.Failure -> ResultType.Failure(
            result.error
        )
    }
}
