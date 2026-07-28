package com.simbiri.presentation.routes.dto.auth

import kotlinx.serialization.Serializable

/**
 * Successful password-authentication response.
 */
@Serializable
data class AuthenticatedSessionResponseDto(
    val userId: String,
    val accessToken: String,
    val tokenType: String,
    val expiresAt: String,
)
