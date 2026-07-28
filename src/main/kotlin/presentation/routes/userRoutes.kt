package com.simbiri.presentation.routes

import com.simbiri.application.user.GetPublicUserByIdUseCase
import com.simbiri.application.user.GetPublicUsersUseCase
import com.simbiri.domain.util.ResultType
import com.simbiri.domain.util.onFailure
import com.simbiri.domain.util.onSuccess
import com.simbiri.presentation.routes.dto.user.publicly.toPublicProfileResponseDto
import com.simbiri.presentation.routes.dto.user.publicly.toPublicProfileResponseDtos
import com.simbiri.presentation.routes.path.UserRoutesPath
import com.simbiri.presentation.utils.parseUserIdOrFailure
import com.simbiri.presentation.utils.parseUserTypeFilterOrFailure
import com.simbiri.presentation.utils.respondWithDataError
import io.ktor.http.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.userRoutes(
    getPublicUserByIdUseCase: GetPublicUserByIdUseCase,
    getPublicUsersUseCase: GetPublicUsersUseCase,
) {

    // GET /users?userType={code}
    get<UserRoutesPath> { path ->
        val userType = when (val parsed = parseUserTypeFilterOrFailure(
            operation = "getUsers",
            rawUserType = path.userType,
        )) {
            is ResultType.Success -> parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@get
            }
        }

        getPublicUsersUseCase(userType).onSuccess { users ->
                call.respond(
                    status = HttpStatusCode.OK,
                    message = users.toPublicProfileResponseDtos(),
                )
            }.onFailure { error ->
                respondWithDataError(error)
            }
    }

    // GET /users/{userId}
    get<UserRoutesPath.ById> { path ->
        val userId = when (val parsed = parseUserIdOrFailure(
            operation = "getUserById",
            rawUserId = path.userId,
        )) {
            is ResultType.Success -> parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@get
            }
        }

        getPublicUserByIdUseCase(userId).onSuccess { user ->
                call.respond(
                    status = HttpStatusCode.OK,
                    message = user.toPublicProfileResponseDto(),
                )
            }.onFailure { error ->
                respondWithDataError(error)
            }
    }

}