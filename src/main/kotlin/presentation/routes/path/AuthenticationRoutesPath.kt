package com.simbiri.presentation.routes.path

import io.ktor.resources.Resource

/**
 * Public authentication endpoints.
 */
@Resource("/auth")
class AuthenticationRoutesPath {

    /**
     * Authenticates a user and issues a short-lived access token.
     */
    @Resource("login")
    data class Login(
        val parent: AuthenticationRoutesPath =
            AuthenticationRoutesPath(),
    )
}
