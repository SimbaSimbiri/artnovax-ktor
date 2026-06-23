package com.simbiri.presentation.utils

import com.simbiri.domain.util.DataError
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.respondWithDataError(errorType: DataError) {
    when(errorType) {
        is DataError.DatabaseError -> {
            call.respond(
                status= HttpStatusCode.InternalServerError,
                message = mapOf(
                    "error" to "DATABASE_ERROR",
                    "message" to "An unexpected database error occurred",
                    "operation" to errorType.operation,
                    "details" to errorType.cause
                )
            )
        }
        DataError.NotFound -> {
            call.respond(
                status= HttpStatusCode.NotFound,
                message = mapOf(
                    "error" to "NOT_FOUND",
                    "message" to "The resource with the specified id/attribute does not exist.",

                    )
                )
        }
        is DataError.UnknownError -> {
            call.respond(
                status= HttpStatusCode.InternalServerError,
                message = mapOf(
                    "error" to "UNKNOWN_ERROR",
                    "message" to "An unknown error occurred.",
                    "details" to errorType.cause
                    )

            )
        }
        is DataError.ValidationError -> {
            call.respond(
                status= HttpStatusCode.BadRequest,
                message = mapOf(
                    "error" to "VALIDATION_ERROR",
                    "message" to errorType.message,
                    )

            )
        }

        is DataError.Conflict -> {
            call.respond(
                status = HttpStatusCode.Conflict,
                message = mapOf(
                    "error" to "CONFLICT",
                    "message" to errorType.message
                )
            )
        }

        is DataError.ForeignKeyViolation -> {
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = mapOf(
                    "error" to "FOREIGN_KEY_VIOLATION",
                    "message" to errorType.message
                )
            )
        }

        is DataError.DuplicateResource -> {
            call.respond(
                status = HttpStatusCode.Conflict,
                message = mapOf(
                    "error" to "DUPLICATE_RESOURCE",
                    "message" to errorType.message
                )
            )
        }
    }

}