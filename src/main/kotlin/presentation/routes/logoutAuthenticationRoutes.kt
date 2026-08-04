package com.simbiri.presentation.routes

import com.simbiri.application.auth.LogoutCurrentDeviceUseCase
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.routes.dto.auth.RefreshTokenRequestDto
import com.simbiri.presentation.routes.path.AuthenticationRoutesPath
import com.simbiri.presentation.utils.respondWithDataError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing

internal typealias LogoutCurrentDeviceHandler = suspend (
    refreshToken: String,
) -> ResultType<Unit, DataError>

/**
 * Public logout route backed by the production application use case.
 */
fun Routing.logoutAuthenticationRoutes(
    logoutCurrentDeviceUseCase: LogoutCurrentDeviceUseCase,
) {
    logoutAuthenticationRoutes(
        logoutCurrentDevice = { refreshToken,->

            logoutCurrentDeviceUseCase(
                refreshToken
            )
        })
}

/**
 * Ends one renewable client login session.
 */
internal fun Routing.logoutAuthenticationRoutes(
    logoutCurrentDevice: LogoutCurrentDeviceHandler,
) {

    // POST /auth/logout
    post<AuthenticationRoutesPath.Logout> {
        call.response.header(
            name = HttpHeaders.CacheControl,
            value = "no-store",
        )

        call.response.header(
            name = HttpHeaders.Pragma,
            value = "no-cache",
        )

        val request = call.receive<RefreshTokenRequestDto>()

        when (val result = logoutCurrentDevice(
            request.refreshToken
        )) {
            is ResultType.Success -> {/*
                 * The client must now remove both the access token and the
                 * refresh token from secure local storage.
                 */
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
