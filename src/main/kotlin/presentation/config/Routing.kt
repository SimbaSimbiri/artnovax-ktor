package com.simbiri.presentation.config

import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.repository.CommunityRepository
import com.simbiri.presentation.routes.communityRoutes
import com.simbiri.presentation.routes.root
import com.simbiri.presentation.routes.userRoutes
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    install(Resources)

    // injecting repos
    val userRepository: UserRepository by inject()
    val communityRepository: CommunityRepository by inject()

    routing {
        // our welcome page
        root()

        // user calls
        userRoutes(userRepository)
        communityRoutes(communityRepository)
    }
}