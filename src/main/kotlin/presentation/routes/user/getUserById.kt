package com.simbiri.presentation.routes.user

import com.simbiri.domain.util.DataError
import com.simbiri.presentation.routes.dto.user.toResponseDto
import com.simbiri.presentation.routes.path.UserRoutesPath
import com.simbiri.presentation.utils.respondWithDataError
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.getUserById() {

    get<UserRoutesPath.ById>{ path->
        val id = path.userId // we can use this to find the user from repository
        val user = repositoryAllUsers.find { it.id.value.toString() == id }

        if(user!=null){
            call.respond(user.toResponseDto())
        } else{
            respondWithDataError(DataError.NotFound)
        }

    }

}