package com.simbiri.presentation.utils

import com.simbiri.domain.model.auth.RefreshAuthenticationError
import com.simbiri.presentation.routes.dto.auth.AuthenticationErrorResponseDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

/**
 * Maps refresh-token failures without exposing whether a token was reused,
 * expired, revoked, stale, malformed, or unknown.
 */
suspend fun RoutingContext.respondWithRefreshAuthenticationError(
    error: RefreshAuthenticationError,
) {
    when (error) {
        RefreshAuthenticationError.InvalidRefreshToken -> {
            call.respond(
                status = HttpStatusCode.Unauthorized,
                message = AuthenticationErrorResponseDto(
                    code = "INVALID_REFRESH_TOKEN",
                    message = "Refresh token is invalid or expired.",
                ),
            )
        }

        is RefreshAuthenticationError.DataFailure -> {
            respondWithDataError(
                error.error
            )
        }
    }
}
