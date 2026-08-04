package com.simbiri.presentation.routes.dto.auth

import kotlinx.serialization.Serializable

/**
 * Successful login or token-refresh response.
 */
@Serializable
data class AuthenticatedSessionResponseDto(
    val userId: String,

    val accessToken: String,
    val tokenType: String,
    val accessTokenExpiresAt: String,

    val refreshToken: String,
    val refreshTokenExpiresAt: String,
)
