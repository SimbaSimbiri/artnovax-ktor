package com.simbiri.presentation.config

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureStatusPages(){
    // this will work hand in hand to deal with incoming invalid json requests to the database
    install(StatusPages) {
        exception<RequestValidationException>{ call, cause ->
            call.respond(message = cause.reasons.joinToString(), status = HttpStatusCode.BadRequest)
        }
    }
}