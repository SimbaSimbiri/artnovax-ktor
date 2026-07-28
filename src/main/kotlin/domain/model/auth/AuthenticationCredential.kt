package com.simbiri.domain.model.auth

import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

/**
 * Authentication state associated one-to-one with a persisted User.
 *
 * Password plaintext must never be placed in this aggregate.
 * passwordHash contains only the encoded output of the configured password hasher.
 */
data class AuthenticationCredential(
    val userId: UserId,

    val passwordHash: String,
    val passwordAlgorithm: PasswordHashAlgorithm,
    val passwordUpdatedAt: Timestamp,

    val failedLoginAttempts: Int = 0,
    val lockedUntil: Timestamp? = null,

    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {

    init {
        require(passwordHash.isNotBlank()) {
            "Authentication credential passwordHash must not be blank."
        }

        require(failedLoginAttempts >= 0) {
            "failedLoginAttempts must not be negative."
        }
    }

    /**
     * Returns true while a temporary authentication lock remains active.
     */
    fun isLockedAt(
        timestamp: Timestamp,
    ): Boolean =
        lockedUntil?.isAfter(timestamp) == true

    /**
     * Safe implicit logging of the automatic hash function of our data class
     */
    override fun toString(): String =
        "AuthenticationCredential(" +
                "userId=$userId, " +
                "passwordHash=<redacted>, " +
                "passwordAlgorithm=$passwordAlgorithm, " +
                "passwordUpdatedAt=$passwordUpdatedAt, " +
                "failedLoginAttempts=$failedLoginAttempts, " +
                "lockedUntil=$lockedUntil, " +
                "createdAt=$createdAt, " +
                "updatedAt=$updatedAt" +
                ")"
}
