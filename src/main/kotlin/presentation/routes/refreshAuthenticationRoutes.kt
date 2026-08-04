package com.simbiri.presentation.routes

import com.simbiri.application.auth.RefreshAccessTokenUseCase
import com.simbiri.domain.model.auth.AuthenticatedSession
import com.simbiri.domain.model.auth.RefreshAuthenticationError
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.routes.dto.auth.RefreshTokenRequestDto
import com.simbiri.presentation.routes.dto.auth.toResponseDto
import com.simbiri.presentation.routes.path.AuthenticationRoutesPath
import com.simbiri.presentation.utils.respondWithRefreshAuthenticationError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing

internal typealias RefreshAccessTokenHandler = suspend (
    refreshToken: String,
) -> ResultType<
        AuthenticatedSession,
        RefreshAuthenticationError,
        >

fun Routing.refreshAuthenticationRoutes(
    refreshAccessTokenUseCase: RefreshAccessTokenUseCase,
) {
    refreshAuthenticationRoutes(
        refreshAccessToken = { refreshToken,->

            refreshAccessTokenUseCase(
                refreshToken
            )
        })
}

/**
 * Public refresh-token rotation endpoint.
 */
internal fun Routing.refreshAuthenticationRoutes(
    refreshAccessToken: RefreshAccessTokenHandler,
) {

    // POST /auth/refresh
    post<AuthenticationRoutesPath.Refresh> {
        call.response.header(
            name = HttpHeaders.CacheControl,
            value = "no-store",
        )

        call.response.header(
            name = HttpHeaders.Pragma,
            value = "no-cache",
        )

        val request = call.receive<RefreshTokenRequestDto>()

        when (val result = refreshAccessToken(
            request.refreshToken
        )) {
            is ResultType.Success -> {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = result.data.toResponseDto(),
                )
            }

            is ResultType.Failure -> {
                respondWithRefreshAuthenticationError(
                    result.error
                )
            }
        }
    }
}
