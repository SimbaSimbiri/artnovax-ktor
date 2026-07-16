package com.simbiri.application.user

import com.simbiri.domain.model.user.User
import com.simbiri.domain.model.user.UserType
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Retrieves users, optionally filtered by user type.
 *
 * Presentation is now responsible for converting query/path values into
 * UserType before calling this use case.
 */
class GetUsersUseCase(
    private val userRepository: UserRepository,
) {

    suspend operator fun invoke(
        userType: UserType? = null,
    ): ResultType<List<User>, DataError> {

        return userRepository.getUsers(
            userType = userType,
        )
    }
}