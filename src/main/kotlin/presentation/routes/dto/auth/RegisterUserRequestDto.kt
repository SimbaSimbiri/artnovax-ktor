package com.simbiri.presentation.routes.dto.auth

import kotlinx.serialization.Serializable

/**
 * Safe public account-registration request.
 *
 * Role, active status, and social links are intentionally not accepted.
 */
@Serializable
data class RegisterUserRequestDto(
    val accountName: String,
    val emailAddress: String,
    val password: String,

    val firstName: String,
    val lastName: String,
    val birthDate: String,

    val about: String? = null,
    val tagline: String? = null,
    val profileImageUrl: String? = null,
    val backgroundImageUrl: String? = null,

    val emailOptIn: Boolean = false,
    val isPrivate: Boolean = true,
    val isAnonymous: Boolean = false,
)
