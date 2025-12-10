package com.simbiri.presentation.routes

import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.onFailure
import com.simbiri.domain.util.onSuccess
import com.simbiri.presentation.routes.dto.user.toResponseDto
import com.simbiri.presentation.routes.path.UserRoutesPath
import com.simbiri.presentation.utils.respondWithDataError
import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing

fun Routing.userRoutes(userRepository: UserRepository) {

    // GET ALL USERS
    get<UserRoutesPath> { path->
        userRepository.getAllUsers(path.userType?.toInt())
            .onSuccess { users->
                call.respond(message= users.toResponseDto(), status = HttpStatusCode.OK)
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    //GET USER BY ID
    get<UserRoutesPath.ById>{ path ->
        userRepository.getUserById(path.userId)
            .onSuccess { user ->
                call.respond(message=user.toResponseDto(), status= HttpStatusCode.OK)
            }
            .onFailure{error ->
                respondWithDataError(error)
            }

    }

}