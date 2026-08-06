package com.simbiri.presentation.utils.therapy

import com.simbiri.domain.model.common.TherapyAssetId
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.util.UUID

fun parseTherapyAssetIdOrFailure(
    operation: String,
    rawTherapyAssetId: String,
): ResultType<TherapyAssetId, DataError> {
    val normalizedId = rawTherapyAssetId.trim()

    if (normalizedId.isEmpty()) {
        return ResultType.Failure(
            DataError.ValidationError(
                message = "$operation failed. A therapy-asset ID is required."
            )
        )
    }

    return try {
        ResultType.Success(
            TherapyAssetId(
                UUID.fromString(normalizedId)
            )
        )
    } catch (_: IllegalArgumentException) {
        ResultType.Failure(
            DataError.ValidationError(
                message = "$operation failed. therapyAssetId='$rawTherapyAssetId' must be a valid UUID."
            )
        )
    }
}
