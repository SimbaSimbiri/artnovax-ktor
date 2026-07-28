package com.simbiri.domain.repository

import com.simbiri.domain.model.auth.UserRegistration
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Creates a User and its initial AuthenticationCredential atomically.
 */
interface UserRegistrationRepository {

    suspend fun register(
        registration: UserRegistration,
    ): ResultType<UserId, DataError>
}
