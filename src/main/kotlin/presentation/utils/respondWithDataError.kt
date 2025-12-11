package com.simbiri.presentation.utils

import com.simbiri.domain.util.DataError
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.respondWithDataError(errorType: DataError) {
    when(errorType) {
        DataError.DatabaseError -> {
            call.respond(
                message= "An unexpected database error occurred.",
                status= HttpStatusCode.InternalServerError
            )
        }
        DataError.NotFound -> {
            call.respond(
                message= "The resource with the specified id/attribute does not exist.",
                status= HttpStatusCode.NotFound
            )
        }
        DataError.UnknownError -> {
            call.respond(
                message= "An unknown error occurred.",
                status= HttpStatusCode.InternalServerError
            )
        }
        DataError.ValidationError -> {
            call.respond(
                message= "The payload/path provided by the client is invalid",
                status= HttpStatusCode.BadRequest
            )
        }
    }

}