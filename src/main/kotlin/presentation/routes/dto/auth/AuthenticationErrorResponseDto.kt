package com.simbiri.presentation.routes.dto.auth

/**
 * Response returned when bearer authentication cannot establish a valid
 * ArtNovaX user identity.
 */
data class AuthenticationErrorResponseDto(
    val code: String = "UNAUTHORIZED",
    val message: String,
)
