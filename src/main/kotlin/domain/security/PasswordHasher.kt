package com.simbiri.domain.security

import com.simbiri.domain.model.auth.PasswordHashAlgorithm

/**
 * Application-facing password hashing contract.
 *
 * Implementations must use an adaptive password-hashing algorithm rather
 * than a fast general-purpose digest such as SHA-256.
 *
 * Caller retains ownership of the supplied CharArray and remains
 * responsible for clearing it after the operation finishes.
 */
interface PasswordHasher {

    val algorithm: PasswordHashAlgorithm

    /**
     * Produces a salted, encoded password hash.
     */
    suspend fun hash(
        password: CharArray,
    ): String

    /**
     * Verifies plaintext password material against an encoded hash.
     */
    suspend fun verify(
        password: CharArray,
        encodedHash: String,
    ): Boolean
}
