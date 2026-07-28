package com.simbiri.presentation.routes.dto.auth

import kotlinx.serialization.Serializable

/**
 * Response returned when bearer authentication cannot establish a valid
 * ArtNovaX user identity.
 */
@Serializable
data class AuthenticationErrorResponseDto(
    val code: String = "UNAUTHORIZED",
    val message: String,
)
