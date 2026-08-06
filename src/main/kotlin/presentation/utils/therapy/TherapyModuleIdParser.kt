package com.simbiri.presentation.utils.therapy

import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.util.UUID

fun parseTherapyModuleIdOrFailure(
    operation: String,
    field: String,
    rawTherapyModuleId: String,
): ResultType<TherapyModuleId, DataError> {
    val normalizedId = rawTherapyModuleId.trim()

    if (normalizedId.isEmpty()) {
        return ResultType.Failure(
            DataError.ValidationError(
                message = "$operation failed. field=$field. A therapy-module ID is required."
            )
        )
    }

    return try {
        ResultType.Success(
            TherapyModuleId(
                UUID.fromString(normalizedId)
            )
        )
    } catch (_: IllegalArgumentException) {
        ResultType.Failure(
            DataError.ValidationError(
                message = "$operation failed. field=$field, value='$rawTherapyModuleId'. " +
                        "Therapy-module ID must be a valid UUID."
            )
        )
    }
}
