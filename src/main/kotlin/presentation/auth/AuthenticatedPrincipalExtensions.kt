package com.simbiri.presentation.auth

import com.simbiri.domain.model.common.UserId
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import com.simbiri.presentation.routes.dto.auth.AuthenticationErrorResponseDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

/**
 * Returns UserId established by JWT authentication.
 *
 * Protected routes must call this inside an authenticate block.
 */
fun ApplicationCall.authenticatedUserIdOrNull(): UserId? = principal<AuthenticatedUserPrincipal>()?.userId

/**
 * Returns the authenticated UserId or completes the request with an
 * Unauthorized response. A defensive fallback of sorts.
 *
 * Routes using this helper must still be inside authenticate(JWT_AUTH_PROVIDER).
 */
suspend fun RoutingContext.authenticatedUserIdOrRespondUnauthorized(): UserId? {
    val userId = call.authenticatedUserIdOrNull()

    if (userId != null) {
        return userId
    }

    call.respond(
        status = HttpStatusCode.Unauthorized,
        message = AuthenticationErrorResponseDto(
            message = "A valid bearer access token is required."
        ),
    )

    return null
}

