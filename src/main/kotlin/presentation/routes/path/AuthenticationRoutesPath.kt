package com.simbiri.presentation.routes.path

import io.ktor.resources.Resource

/**
 * Public authentication endpoints.
 */
@Resource("/auth")
class AuthenticationRoutesPath {

    /**
     * Creates a new regular user account and password credential.
     */
    @Resource("register")
    data class Register(
        val parent: AuthenticationRoutesPath =
            AuthenticationRoutesPath(),
    )

    /**
     * Authenticates a user and issues a short-lived access token.
     */
    @Resource("login")
    data class Login(
        val parent: AuthenticationRoutesPath =
            AuthenticationRoutesPath(),
    )
}
