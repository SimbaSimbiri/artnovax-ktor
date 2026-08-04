package com.simbiri.presentation.routes.dto.auth

import kotlinx.serialization.Serializable

/**
 * Opaque refresh token supplied for one-time rotation.
 *
 * This DTO must never be logged.
 */
@Serializable
data class RefreshTokenRequestDto(
    val refreshToken: String,
)
