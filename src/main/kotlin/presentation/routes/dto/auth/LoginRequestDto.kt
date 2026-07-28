package com.simbiri.presentation.routes.dto.auth

import kotlinx.serialization.Serializable

/**
 * Credentials supplied to the password-authentication endpoint.
 *
 * The request object is not logged because password is plaintext.
 */
@Serializable
data class LoginRequestDto(
    val emailAddress: String,
    val password: String,
)
