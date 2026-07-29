package com.simbiri.presentation.utils

import com.simbiri.domain.model.auth.PasswordChangeError
import com.simbiri.presentation.routes.dto.auth.AuthenticationErrorResponseDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

/**
 * Maps password-change failures to HTTP responses.
 */
suspend fun RoutingContext.respondWithPasswordChangeError(
    error: PasswordChangeError,
) {
    when (error) {
        PasswordChangeError.InvalidCurrentPassword,
        PasswordChangeError.TemporarilyLocked,
            -> {/*
             * These cases intentionally share one response so callers
             * cannot determine whether the credential is temporarily locked.
             */
            call.respond(
                status = HttpStatusCode.Forbidden,
                message = AuthenticationErrorResponseDto(
                    code = "CURRENT_PASSWORD_NOT_VERIFIED",
                    message = "Current password could not be verified.",
                ),
            )
        }

        PasswordChangeError.NewPasswordMatchesCurrent -> {
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = AuthenticationErrorResponseDto(
                    code = "PASSWORD_REUSE_NOT_ALLOWED",
                    message = "New password must differ from " + "the current password.",
                ),
            )
        }

        is PasswordChangeError.ValidationFailure -> {
            respondWithDataError(
                error.error
            )
        }

        is PasswordChangeError.DataFailure -> {
            respondWithDataError(
                error.error
            )
        }
    }
}
