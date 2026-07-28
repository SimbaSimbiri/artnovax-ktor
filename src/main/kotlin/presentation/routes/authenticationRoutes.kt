package com.simbiri.presentation.routes

import com.simbiri.application.auth.AuthenticateUserUseCase
import com.simbiri.domain.model.auth.AuthenticatedSession
import com.simbiri.domain.model.auth.AuthenticationError
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.routes.dto.auth.LoginRequestDto
import com.simbiri.presentation.routes.dto.auth.toResponseDto
import com.simbiri.presentation.routes.path.AuthenticationRoutesPath
import com.simbiri.presentation.utils.respondWithAuthenticationError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing

/**
 * Function contract used by the route after HTTP parsing.
 *
 * Keeping this seam internal allows focused route tests without starting
 * Koin, PostgreSQL, or the complete authentication infrastructure.
 */
internal typealias AuthenticateUserHandler = suspend (
    emailAddress: String,
    password: CharArray,
) -> ResultType< AuthenticatedSession, AuthenticationError,>

/**
 * Public authentication routes backed by the production use case.
 */
fun Routing.authenticationRoutes(
    authenticateUserUseCase: AuthenticateUserUseCase,
) {
    authenticationRoutes(
        authenticateUser = { emailAddress, password, ->

            authenticateUserUseCase(
                emailAddress = emailAddress,
                password = password,
            )
        })
}

/**
 * Public password-authentication endpoint.
 */
internal fun Routing.authenticationRoutes(
    authenticateUser: AuthenticateUserHandler,
) {

    // POST /auth/login
    post<AuthenticationRoutesPath.Login> {
        /*
         * Authentication responses must not be stored by browsers, proxies, or intermediary caches.
         */
        call.response.header(
            name = HttpHeaders.CacheControl,
            value = "no-store",
        )

        call.response.header(
            name = HttpHeaders.Pragma,
            value = "no-cache",
        )

        val request = call.receive<LoginRequestDto>()
        val workingPassword = request.password.toCharArray()

        try {
            when (val result = authenticateUser(request.emailAddress,workingPassword,)) {
                is ResultType.Success -> {
                    call.respond(
                        status = HttpStatusCode.OK,

                        message = result.data.toResponseDto(),
                    )
                }

                is ResultType.Failure -> {
                    respondWithAuthenticationError(
                        result.error
                    )
                }
            }
        } finally {
            workingPassword.fill('\u0000')
        }
    }
}
