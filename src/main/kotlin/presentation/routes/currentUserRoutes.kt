package com.simbiri.presentation.routes

import com.simbiri.application.user.GetCurrentUserUseCase
import com.simbiri.domain.util.onFailure
import com.simbiri.domain.util.onSuccess
import com.simbiri.presentation.auth.authenticatedUserIdOrRespondUnauthorized
import com.simbiri.presentation.routes.dto.user.current.toCurrentUserResponseDto
import com.simbiri.presentation.routes.path.CurrentUserRoutesPath
import com.simbiri.presentation.utils.respondWithDataError
import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing

/**
 * Authenticated current-user profile routes.
 */
fun Routing.currentUserRoutes(
    getCurrentUserUseCase: GetCurrentUserUseCase,
) {

    // GET /me
    get<CurrentUserRoutesPath> {
        val authenticatedUserId = authenticatedUserIdOrRespondUnauthorized() ?: return@get

        getCurrentUserUseCase(
            authenticatedUserId
        ).onSuccess { user ->
                call.respond(
                    status = HttpStatusCode.OK,
                    message = user.toCurrentUserResponseDto(),
                )
            }.onFailure { error ->
                respondWithDataError(error)
            }
    }
}
