package com.simbiri.presentation.routes

import com.simbiri.application.user.GetCurrentUserUseCase
import com.simbiri.application.user.UpdateCurrentUserProfileUseCase
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
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.routes.dto.user.current.CurrentUserUpdateRequestDto
import com.simbiri.presentation.routes.dto.user.current.toCurrentUserProfileUpdate
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.resources.put
import io.ktor.server.response.header

/**
 * Authenticated current-user profile routes.
 */
fun Routing.currentUserRoutes(
    getCurrentUserUseCase: GetCurrentUserUseCase,
    updateCurrentUserProfileUseCase: UpdateCurrentUserProfileUseCase,
) {

    // GET /me
    get<CurrentUserRoutesPath> {
        call.preventSensitiveProfileCaching()
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

    // PUT /me
    put<CurrentUserRoutesPath> {
        call.preventSensitiveProfileCaching()
        val authenticatedUserId = authenticatedUserIdOrRespondUnauthorized() ?: return@put

        val request = call.receive<CurrentUserUpdateRequestDto>()

        val profileUpdate = when (val parsed = request.toCurrentUserProfileUpdate()) {
            is ResultType.Success -> parsed.data

            is ResultType.Failure -> {
                respondWithDataError(
                    parsed.error
                )

                return@put
            }
        }

        updateCurrentUserProfileUseCase(
            authenticatedUserId = authenticatedUserId,
            profileUpdate = profileUpdate,
        ).onSuccess { updatedUser ->
            call.respond(
                status = HttpStatusCode.OK,
                message = updatedUser.toCurrentUserResponseDto(),
            )
        }.onFailure { error ->
            respondWithDataError(error)
        }
    }
}

/**
 * Prevents authenticated profile data from being stored by clients,
 * proxies, or intermediary caches.
 */
private fun ApplicationCall.preventSensitiveProfileCaching() {
    response.header(
        name = HttpHeaders.CacheControl,
        value = "no-store",
    )

    response.header(
        name = HttpHeaders.Pragma,
        value = "no-cache",
    )
}