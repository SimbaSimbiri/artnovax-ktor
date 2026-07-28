package com.simbiri.domain.model.auth

import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

/**
 * Successful result of the password-authentication workflow.
 */
data class AuthenticatedSession(
    val userId: UserId,
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresAt: Timestamp,
)
