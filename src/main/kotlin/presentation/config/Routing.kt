package com.simbiri.presentation.config

import com.simbiri.presentation.routes.root
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.resources.Resources
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    install(Resources)

    routing {
        // our welcome page
        root()
    }
}