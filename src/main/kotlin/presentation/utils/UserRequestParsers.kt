package com.simbiri.presentation.utils

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.user.UserType
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.util.UUID

/**
 * Converts a raw HTTP path value into a typed UserId.
 *
 * This belongs to the presentation layer because the raw string originates
 * from an HTTP request rather than from the domain or persistence layer.
 */
fun parseUserIdOrFailure(
    operation: String,
    rawUserId: String?,
): ResultType<UserId, DataError> {
    if (rawUserId.isNullOrBlank()) {
        return ResultType.Failure(
            DataError.ValidationError(
                message = "User ID validation failed in $operation. " +
                        "Field 'userId' is required and cannot be blank. " +
                        "receivedValue='${rawUserId ?: "null"}'."
            )
        )
    }

    val uuid = runCatching {
        UUID.fromString(rawUserId)
    }.getOrNull()
        ?: return ResultType.Failure(
            DataError.ValidationError(
                message = "User ID validation failed in $operation. " +
                        "Field 'userId' must be a valid UUID. " +
                        "receivedValue='$rawUserId'."
            )
        )

    return ResultType.Success(
        UserId(uuid)
    )
}

/**
 * Converts the optional userType query parameter into a domain UserType.
 *
 * A null value is valid and means that no type filter was supplied.
 */
fun parseUserTypeFilterOrFailure(
    operation: String,
    rawUserType: String?,
): ResultType<UserType?, DataError> {
    if (rawUserType == null) {
        return ResultType.Success(null)
    }

    if (rawUserType.isBlank()) {
        return ResultType.Failure(
            DataError.ValidationError(
                message = "User-type validation failed in $operation. " +
                        "Query parameter 'userType' cannot be blank when provided. " +
                        "receivedValue='$rawUserType'."
            )
        )
    }

    val userTypeCode = rawUserType.toIntOrNull()
        ?: return ResultType.Failure(
            DataError.ValidationError(
                message = "User-type validation failed in $operation. " +
                        "Query parameter 'userType' must be a valid integer. " +
                        "receivedValue='$rawUserType'."
            )
        )

    val userType = UserType.fromCodeOrNull(userTypeCode)
        ?: return ResultType.Failure(
            DataError.ValidationError(
                message = "User-type validation failed in $operation. " +
                        "Query parameter 'userType' contains an unsupported code. " +
                        "receivedValue='$rawUserType', " +
                        "parsedCode=$userTypeCode, " +
                        "supportedCodes=${UserType.entries.map { it.code }}."
            )
        )

    return ResultType.Success(userType)
}