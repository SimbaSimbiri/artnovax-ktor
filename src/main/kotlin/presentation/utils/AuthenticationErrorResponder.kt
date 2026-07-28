package com.simbiri.presentation.utils

import com.simbiri.domain.model.auth.AuthenticationError
import com.simbiri.presentation.routes.dto.auth.AuthenticationErrorResponseDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

/**
 * Converts authentication application failures into HTTP responses.
 *
 * Invalid credentials and temporary account locks produce the
 * same status and response body so the endpoint does not disclose account
 * existence or lock state.
 */

suspend fun RoutingContext.respondWithAuthenticationError(
    error: AuthenticationError,
) {
    when (error) {
        AuthenticationError.InvalidCredentials,
        AuthenticationError.TemporarilyLocked,
            -> {
            call.respond(
                status = HttpStatusCode.Unauthorized,
                message = AuthenticationErrorResponseDto(
                    code = "INVALID_CREDENTIALS",
                    message = "Email address or password is incorrect.",
                ),
            )
        }

        is AuthenticationError.DataFailure -> {/*
             * Unexpected repository and infrastructure errors continue
             * through the application's standard DataError mapping.
             */
            respondWithDataError(
                error.error
            )
        }
    }
}
