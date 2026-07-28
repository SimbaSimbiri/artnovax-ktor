package com.simbiri.presentation.routes.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterUserResponseDto(
    val userId: String,
)
