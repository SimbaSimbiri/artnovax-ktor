package com.simbiri.presentation.config

import com.simbiri.data.repository.UserRepoImpl
import com.simbiri.presentation.routes.root
import com.simbiri.presentation.routes.userRoutes
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(Resources)
    val userRepository = UserRepoImpl()

    routing {
        // our welcome page
        root()

        // user calls
        userRoutes(userRepository)
    }
}