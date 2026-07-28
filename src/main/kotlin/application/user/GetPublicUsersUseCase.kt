package com.simbiri.application.user

import com.simbiri.domain.model.user.User
import com.simbiri.domain.model.user.UserType
import com.simbiri.domain.policy.user.UserProfileVisibilityPolicy
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Retrieves users that may be shown through the public profile catalogue.
 */
class GetPublicUsersUseCase(
    private val userRepository: UserRepository,
) {

    suspend operator fun invoke(
        userType: UserType? = null,
    ): ResultType<List<User>, DataError> = when (val result = userRepository.getUsers(
        userType
    )) {
        is ResultType.Success -> ResultType.Success(
            result.data.filter(
                UserProfileVisibilityPolicy::isPubliclyVisible
            )
        )

        is ResultType.Failure -> if (result.error == DataError.NotFound) {
            ResultType.Success(
                emptyList()
            )
        } else {
            ResultType.Failure(
                result.error
            )
        }
    }
}
