package com.simbiri.domain.policy.auth

import com.simbiri.domain.model.auth.AuthenticationCredential
import com.simbiri.domain.model.auth.PasswordHashAlgorithm
import com.simbiri.domain.model.common.UserId
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthenticationAttemptPolicyTest {

    @Test
    fun `fifth failed attempt temporarily locks credential`() {
        val attemptedAt = Instant.parse(
            "2026-07-28T18:00:00Z"
        )

        var credential = credential()

        repeat(
            AuthenticationAttemptPolicy.MAXIMUM_FAILED_ATTEMPTS
        ) {
            credential = AuthenticationAttemptPolicy.afterFailedAttempt(
                    credential = credential,
                    attemptedAt = attemptedAt,
                )
        }

        assertTrue(
            credential.isLockedAt(
                attemptedAt
            )
        )

        assertEquals(
            expected = 0,
            actual = credential.failedLoginAttempts,
        )
    }

    @Test
    fun `successful attempt clears accumulated failures`() {
        val credential = credential().copy(
            failedLoginAttempts = 3,
        )

        val updatedCredential = AuthenticationAttemptPolicy.afterSuccessfulAttempt(
                credential
            )

        assertEquals(
            expected = 0,
            actual = updatedCredential.failedLoginAttempts,
        )

        assertFalse(
            updatedCredential.isLockedAt(
                Instant.parse(
                    "2026-07-28T18:00:00Z"
                )
            )
        )
    }

    @Test
    fun `expired lock begins a new failed-attempt window`() {
        val attemptedAt = Instant.parse(
            "2026-07-28T18:00:00Z"
        )

        val expiredCredential = credential().copy(
            failedLoginAttempts = 0,
            lockedUntil = attemptedAt.minusSeconds(1),
        )

        val updatedCredential = AuthenticationAttemptPolicy.afterFailedAttempt(
                credential = expiredCredential,
                attemptedAt = attemptedAt,
            )

        assertEquals(
            expected = 1,
            actual = updatedCredential.failedLoginAttempts,
        )

        assertFalse(
            updatedCredential.isLockedAt(
                attemptedAt
            )
        )
    }

    private fun credential(): AuthenticationCredential = AuthenticationCredential(
        userId = UserId(
            UUID.randomUUID()
        ),

        passwordHash = "\$argon2id\$test-hash",

        passwordAlgorithm = PasswordHashAlgorithm.ARGON2ID,

        passwordUpdatedAt = Instant.parse(
            "2026-07-28T12:00:00Z"
        ),

        failedLoginAttempts = 0,
        lockedUntil = null,

        createdAt = Instant.parse(
            "2026-07-28T12:00:00Z"
        ),

        updatedAt = Instant.parse(
            "2026-07-28T12:00:00Z"
        ),
    )
}
