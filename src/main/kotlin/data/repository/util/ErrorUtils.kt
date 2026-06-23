package com.simbiri.data.repository.util

import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.util.UUID

fun validationError(
    operation: String,
    field: String,
    value: String?,
    reason: String,
): DataError.ValidationError = DataError.ValidationError(
    message = "Validation failed in $operation. Field '$field' is invalid. " +
            "Received value: '${value ?: "null"}'. Reason: $reason"
)

fun databaseError(
    operation: String,
    e: Exception,
    details: String? = null,
): DataError.DatabaseError = DataError.DatabaseError(
    operation = operation,
    cause = buildString {
        append(e::class.simpleName ?: "Exception")
        e.message?.let { append(": ").append(it) }
        details?.let { append(" | Details: ").append(it) }
    }
)

fun conflictError(
    operation: String,
    message: String,
): DataError.Conflict = DataError.Conflict(
    message = "Conflict in $operation.\n$message",
)

fun foreignKeyError(
    operation: String,
    message: String,
): DataError.ForeignKeyViolation = DataError.ForeignKeyViolation(
    message = "Foreign key violation in $operation.\n$message"
)

fun duplicateResourceError(
    operation: String,
    message: String,
): DataError.DuplicateResource = DataError.DuplicateResource(
    message = "Duplicate resource in $operation.\n$message"
)

fun unknownError(
    operation: String,
    e: Exception,
    details: String? = null
): DataError.UnknownError = DataError.UnknownError(
    cause = buildString {
        append("Unknown error in $operation.")
        append(e::class.simpleName ?: "Exception")
        e.message?.let { append(": ").append(it) }
        details.let { append(" | Details: ").append(it) }
    }
)

fun parseUuidOrFailure(
    operation: String,
    field: String,
    value: String?,
): ResultType<UUID, DataError> {
    if (value.isNullOrBlank()) {
        return ResultType.Failure(
            validationError(
                operation = operation,
                field = field,
                value = value,
                reason = "Field '$field' is required and cannot be blank"
            )
        )
    }
    val uuid = runCatching { UUID.fromString(value) }.getOrNull()
        ?: return ResultType.Failure(
            validationError(
                operation = operation,
                field = field,
                value = value,
                reason = "$field must be a valid UUID"
            )
        )
    return ResultType.Success(uuid)
}
