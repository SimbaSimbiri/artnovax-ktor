package com.simbiri.presentation.routes

import com.simbiri.application.auth.LogoutAllDevicesUseCase
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.auth.authenticatedUserIdOrRespondUnauthorized
import com.simbiri.presentation.routes.path.CurrentUserRoutesPath
import com.simbiri.presentation.utils.respondWithDataError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.delete
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing

internal typealias LogoutAllDevicesHandler = suspend (
    authenticatedUserId: UserId,
) -> ResultType<Unit, DataError>

/**
 * Current-user session routes
 */
fun Routing.currentUserSessionRoutes(
    logoutAllDevicesUseCase: LogoutAllDevicesUseCase,
) {
    currentUserSessionRoutes(
        logoutAllDevices = { authenticatedUserId,->

            logoutAllDevicesUseCase(
                authenticatedUserId
            )
        })
}

/**
 * Invalidates every access token issued for the JWT principal.
 */
internal fun Routing.currentUserSessionRoutes(
    logoutAllDevices: LogoutAllDevicesHandler,
) {

    // DELETE /me/sessions
    delete<CurrentUserRoutesPath.Sessions> {
        call.response.header(
            name = HttpHeaders.CacheControl,
            value = "no-store",
        )

        call.response.header(
            name = HttpHeaders.Pragma,
            value = "no-cache",
        )

        val authenticatedUserId = authenticatedUserIdOrRespondUnauthorized() ?: return@delete

        when (val result = logoutAllDevices(
            authenticatedUserId
        )) {
            is ResultType.Success -> {
                // Bearer token is stale immediately after the repository increments sessionVersion.
                call.respond(
                    HttpStatusCode.NoContent
                )
            }

            is ResultType.Failure -> {
                respondWithDataError(
                    result.error
                )
            }
        }
    }
}
