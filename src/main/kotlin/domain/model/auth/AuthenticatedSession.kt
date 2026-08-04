package com.simbiri.domain.model.auth

import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

/**
 * Successful authentication or refresh result.
 *
 * Both token values are sensitive and must never be logged.
 */
data class AuthenticatedSession(
    val userId: UserId,

    val accessToken: String,
    val tokenType: String = "Bearer",
    val accessTokenExpiresAt: Timestamp,

    val refreshToken: String,
    val refreshTokenExpiresAt: Timestamp,
) {

    /**
     * Prevents logging sensitive tokens.
     */
    override fun toString(): String =
        "AuthenticatedSession(userId=$userId, accessToken=<redacted>, tokenType=$tokenType," +
                " accessTokenExpiresAt=$accessTokenExpiresAt, refreshToken=<redacted>, " +
                "refreshTokenExpiresAt=$refreshTokenExpiresAt)"
}
