package com.simbiri.presentation.routes.dto.auth

import kotlinx.serialization.Serializable

/**
 * Password material supplied by an authenticated user.
 *
 * This DTO and its contents must never be logged.
 */
@Serializable
data class ChangePasswordRequestDto(
    val currentPassword: String,
    val newPassword: String,
)
