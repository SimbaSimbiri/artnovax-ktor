package com.simbiri.domain.repository

import com.simbiri.domain.model.auth.AuthenticationAttemptMutationResult
import com.simbiri.domain.model.auth.PasswordHashAlgorithm
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Performs narrow, concurrency-safe authentication credential mutations.
 *
 * These operations must not overwrite unrelated credential fields.
 */
interface AuthenticationCredentialMutationRepository {

    /**
     * Records one failed authentication attempt against an expected
     * credential snapshot.
     */
    suspend fun recordFailedLoginAttempt(
        userId: UserId,
        expectedPasswordHash: String,
        expectedSessionVersion: Long,
        attemptedAt: Timestamp,
    ): ResultType<
            AuthenticationAttemptMutationResult,
            DataError,
            >

    /**
     * Clears failed-attempt state following successful password
     * authentication.
     */
    suspend fun recordSuccessfulLogin(
        userId: UserId,
        expectedPasswordHash: String,
        expectedSessionVersion: Long,
        authenticatedAt: Timestamp,
    ): ResultType<
            AuthenticationAttemptMutationResult,
            DataError,
            >

    /**
     * Replaces the password and increments sessionVersion in one SQL update.
     *
     * The expected password hash and session version provide optimistic
     * concurrency protection.
     */
    suspend fun replacePasswordAndIncrementSessionVersion(
        userId: UserId,
        expectedPasswordHash: String,
        expectedSessionVersion: Long,
        passwordHash: String,
        passwordAlgorithm: PasswordHashAlgorithm,
        passwordUpdatedAt: Timestamp,
    ): ResultType<Long, DataError>
}
