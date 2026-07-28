package com.simbiri.presentation.routes.dto.auth

import com.simbiri.domain.model.auth.AuthenticatedSession

/**
 * Maps an authenticated application session to its HTTP representation.
 */
fun AuthenticatedSession.toResponseDto(): AuthenticatedSessionResponseDto = AuthenticatedSessionResponseDto(
    userId = userId.value.toString(),
    accessToken = accessToken,
    tokenType = tokenType,
    expiresAt = expiresAt.toString(),
)
