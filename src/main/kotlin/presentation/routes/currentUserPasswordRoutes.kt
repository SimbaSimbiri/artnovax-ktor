package com.simbiri.presentation.routes

import com.simbiri.application.auth.ChangeCurrentUserPasswordUseCase
import com.simbiri.domain.model.auth.PasswordChangeError
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.auth.authenticatedUserIdOrRespondUnauthorized
import com.simbiri.presentation.routes.dto.auth.ChangePasswordRequestDto
import com.simbiri.presentation.routes.path.CurrentUserRoutesPath
import com.simbiri.presentation.utils.respondWithPasswordChangeError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.put
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing

internal typealias ChangeCurrentUserPasswordHandler = suspend (
    authenticatedUserId: UserId,
    currentPassword: CharArray,
    newPassword: CharArray,
) -> ResultType<Unit, PasswordChangeError>

/**
 * Authenticated password route backed by the production use case.
 */
fun Routing.currentUserPasswordRoutes(
    changeCurrentUserPasswordUseCase: ChangeCurrentUserPasswordUseCase,
) {
    currentUserPasswordRoutes(
        changePassword = {
                authenticatedUserId,
                currentPassword,
                newPassword,
            ->

            changeCurrentUserPasswordUseCase(
                authenticatedUserId = authenticatedUserId,
                currentPassword = currentPassword,
                newPassword = newPassword,
            )
        })
}

/**
 * Changes the credential belonging to the JWT principal.
 */
internal fun Routing.currentUserPasswordRoutes(
    changePassword: ChangeCurrentUserPasswordHandler,
) {

    // PUT /me/password
    put<CurrentUserRoutesPath.Password> {
        call.response.header(
            name = HttpHeaders.CacheControl,
            value = "no-store",
        )

        call.response.header(
            name = HttpHeaders.Pragma,
            value = "no-cache",
        )

        val authenticatedUserId = authenticatedUserIdOrRespondUnauthorized() ?: return@put

        val request = call.receive<ChangePasswordRequestDto>()

        val currentPassword = request.currentPassword.toCharArray()
        val newPassword = request.newPassword.toCharArray()

        try {
            when (val result = changePassword(
                authenticatedUserId,
                currentPassword,
                newPassword,
            )) {
                is ResultType.Success -> {
                    call.respond(
                        HttpStatusCode.NoContent
                    )
                }

                is ResultType.Failure -> {
                    respondWithPasswordChangeError(
                        result.error
                    )
                }
            }
        } finally {/*
             * Clear both route-owned plaintext working copies after every
             * outcome.
             */
            currentPassword.fill('\u0000')
            newPassword.fill('\u0000')
        }
    }
}
