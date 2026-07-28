package com.simbiri.domain.repository

import com.simbiri.domain.model.auth.AuthenticationCredential
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Persistence operations for the AuthenticationCredential aggregate.
 */
interface AuthenticationCredentialRepository {

    suspend fun getCredentialByUserId(
        userId: UserId,
    ): ResultType<AuthenticationCredential, DataError>

    suspend fun createCredential(
        credential: AuthenticationCredential,
    ): ResultType<Unit, DataError>

    suspend fun updateCredential(
        credential: AuthenticationCredential,
    ): ResultType<Unit, DataError>

    suspend fun deleteCredentialByUserId(
        userId: UserId,
    ): ResultType<Unit, DataError>
}
