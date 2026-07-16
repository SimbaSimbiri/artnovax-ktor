package com.simbiri.application.user

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.user.User
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Retrieves one user by ID
 *
 * Accepts a typed UserId rather than http path param string.
 * Parsing and validating the path params is now delegated to presentation
 */
class GetUserByIdUseCase( private val userRepository: UserRepository ) {

    suspend operator fun invoke(userId: UserId,): ResultType<User, DataError> {

        return userRepository.getUserById(userId = userId)
    }
    /*
    Later, this is where we will add operation-level concerns such as:

    - checking whether the requester may view a private user
    - hiding deactivated accounts
    - audit logging
    - field visibility policies
    - retrieving the current authenticated user
    - applying profile access rules
     */
}