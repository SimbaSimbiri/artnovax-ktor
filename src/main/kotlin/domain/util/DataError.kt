package com.simbiri.domain.util

sealed interface DataError : Error {
    data object NotFound : DataError
    data class DatabaseError (
        val operation: String,
        val cause: String? = null
    ): DataError
    data class ValidationError(
        val message: String,
    ) : DataError
    data class UnknownError(
        val cause: String?= null
    ) : DataError

    data class Conflict(
        val message: String
    ) : DataError

    data class Forbidden(
        val message: String,
    ): DataError

    data class ForeignKeyViolation(
        val message: String
    ) : DataError

    data class DuplicateResource(
        val message: String
    ) : DataError
}