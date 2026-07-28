package com.simbiri.presentation.routes

import com.simbiri.application.auth.RegisterUserUseCase
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.user.User
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.routes.dto.auth.RegisterUserRequestDto
import com.simbiri.presentation.routes.dto.auth.RegisterUserResponseDto
import com.simbiri.presentation.routes.dto.auth.toRegistrationUser
import com.simbiri.presentation.routes.path.AuthenticationRoutesPath
import com.simbiri.presentation.utils.respondWithDataError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing

internal typealias RegisterUserHandler = suspend (
    user: User,
    password: CharArray,
) -> ResultType<UserId, DataError>

/**
 * Public registration route backed by the production use case.
 */
fun Routing.registrationRoutes(
    registerUserUseCase: RegisterUserUseCase,
) {
    registrationRoutes(
        registerUser = {
                user,
                password,
            ->

            registerUserUseCase(
                user = user,
                password = password,
            )
        })
}

/**
 * Creates a REGULAR user and password credential atomically.
 */
internal fun Routing.registrationRoutes(
    registerUser: RegisterUserHandler,
) {

    // POST /auth/register
    post<AuthenticationRoutesPath.Register> {
        call.response.header(
            name = HttpHeaders.CacheControl,
            value = "no-store",
        )

        call.response.header(
            name = HttpHeaders.Pragma,
            value = "no-cache",
        )

        val request = call.receive<RegisterUserRequestDto>()

        val user = request.toRegistrationUser()
        val workingPassword = request.password.toCharArray()

        try {
            when (val result = registerUser(
                user,
                workingPassword,
            )) {
                is ResultType.Success -> {
                    call.respond(
                        status = HttpStatusCode.Created,

                        message = RegisterUserResponseDto(
                            userId = result.data.value.toString()
                        ),
                    )
                }

                is ResultType.Failure -> {
                    respondWithDataError(
                        result.error
                    )
                }
            }
        } finally {
            workingPassword.fill('\u0000')
        }
    }
}
