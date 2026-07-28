package com.simbiri.domain.policy.auth

import com.simbiri.domain.model.auth.AuthenticationCredential
import java.time.Duration
import java.time.Instant

/**
 * Controls temporary locking after repeated failed password attempts.
 */
object AuthenticationAttemptPolicy {

    /**
     * Records one failed password verification.
     *
     * An expired lock begins a fresh attempt window
     */
    fun afterFailedAttempt(
        credential: AuthenticationCredential,
        attemptedAt: Instant,
    ): AuthenticationCredential {
        val currentCredential = clearExpiredLock(
            credential = credential,
            checkedAt = attemptedAt,
        )

        val nextFailedAttemptCount = currentCredential.failedLoginAttempts + 1

        return if (nextFailedAttemptCount >= MAXIMUM_FAILED_ATTEMPTS) {
            currentCredential.copy(
                failedLoginAttempts = 0,
                lockedUntil = attemptedAt.plus(
                    LOCK_DURATION
                ),
            )
        } else {
            currentCredential.copy(
                failedLoginAttempts = nextFailedAttemptCount,
                lockedUntil = null,
            )
        }
    }

    /**
     * Clears accumulated failures after successful authentication.
     */
    fun afterSuccessfulAttempt(
        credential: AuthenticationCredential,
    ): AuthenticationCredential = credential.copy(
        failedLoginAttempts = 0,
        lockedUntil = null,
    )

    private fun clearExpiredLock(
        credential: AuthenticationCredential,
        checkedAt: Instant,
    ): AuthenticationCredential {
        val lockedUntil = credential.lockedUntil ?: return credential

        return if (lockedUntil.isAfter(checkedAt)) {
            credential
        } else {
            credential.copy(
                failedLoginAttempts = 0,
                lockedUntil = null,
            )
        }
    }

    const val MAXIMUM_FAILED_ATTEMPTS = 5

    private val LOCK_DURATION = Duration.ofMinutes(15)
}
