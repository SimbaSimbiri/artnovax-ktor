package com.simbiri.domain.security

import com.simbiri.domain.model.auth.IssuedRefreshToken

/**
 * Generates and hashes opaque refresh tokens.
 */
interface RefreshTokenIssuer {

    /**
     * Generates a new unpredictable token and its persistence-safe digest.
     */
    fun issue(): IssuedRefreshToken

    /**
     * Hashes a client-supplied token for repository lookup.
     *
     * The plaintext value must not be logged.
     */
    fun hash(
        tokenValue: String,
    ): String
}
