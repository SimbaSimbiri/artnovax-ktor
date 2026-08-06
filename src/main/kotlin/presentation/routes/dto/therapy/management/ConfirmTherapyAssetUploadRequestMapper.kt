package com.simbiri.presentation.routes.dto.therapy.management

import com.simbiri.application.therapy.asset.ConfirmTherapyAssetUploadRequest
import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.therapy.TherapyAssetRole
import com.simbiri.domain.model.therapy.TherapyMediaType
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.utils.therapy.parseTherapyModuleIdOrFailure
import java.util.Locale
import kotlin.enums.enumEntries

fun ConfirmTherapyAssetUploadRequestDto.toConfirmationRequestOrFailure(
    therapySessionId: TherapySessionId,
): ResultType<ConfirmTherapyAssetUploadRequest, DataError> {
    val parsedModuleId = when (
        val result = parseOptionalModuleIdOrFailure(therapyModuleId)
    ) {
        is ResultType.Success -> result.data
        is ResultType.Failure -> return ResultType.Failure(result.error)
    }

    val parsedRole = when (
        val result = parseConfirmationEnumOrFailure<TherapyAssetRole>(
            field = "role",
            rawValue = role,
        )
    ) {
        is ResultType.Success -> result.data
        is ResultType.Failure -> return ResultType.Failure(result.error)
    }

    val parsedMediaType = when (
        val result = parseConfirmationEnumOrFailure<TherapyMediaType>(
            field = "mediaType",
            rawValue = mediaType,
        )
    ) {
        is ResultType.Success -> result.data
        is ResultType.Failure -> return ResultType.Failure(result.error)
    }

    return ResultType.Success(
        ConfirmTherapyAssetUploadRequest(
            therapySessionId = therapySessionId,
            therapyModuleId = parsedModuleId,
            role = parsedRole,
            mediaType = parsedMediaType,
            storageKey = storageKey.trim(),
            mimeType = mimeType.trim(),
            sizeBytes = sizeBytes,
            sha256 = sha256.trim(),
            locale = locale?.trim()?.takeIf(String::isNotEmpty),
            altText = altText?.trim()?.takeIf(String::isNotEmpty),
            transcript = transcript?.trim()?.takeIf(String::isNotEmpty),
        )
    )
}

private fun parseOptionalModuleIdOrFailure(
    rawTherapyModuleId: String?,
): ResultType<TherapyModuleId?, DataError> {
    if (rawTherapyModuleId == null) {
        return ResultType.Success(null)
    }

    return when (
        val result = parseTherapyModuleIdOrFailure(
            operation = "confirmTherapyAssetUpload",
            field = "therapyModuleId",
            rawTherapyModuleId = rawTherapyModuleId,
        )
    ) {
        is ResultType.Success -> ResultType.Success(result.data)
        is ResultType.Failure -> ResultType.Failure(result.error)
    }
}

private inline fun <reified E : Enum<E>> parseConfirmationEnumOrFailure(
    field: String,
    rawValue: String,
): ResultType<E, DataError> {
    val normalizedValue = rawValue.trim()
        .uppercase(Locale.ROOT)
        .replace(CONFIRMATION_ENUM_SEPARATOR_PATTERN, "_")

    val parsedValue = enumEntries<E>().firstOrNull { value ->
        value.name == normalizedValue
    }

    if (parsedValue != null) {
        return ResultType.Success(parsedValue)
    }

    val allowedValues = enumEntries<E>().joinToString(", ") { value ->
        value.name
    }

    return ResultType.Failure(
        DataError.ValidationError(
            message = "Therapy asset confirmation is invalid. field=$field, value='$rawValue'. " +
                    "Allowed values: $allowedValues."
        )
    )
}

private val CONFIRMATION_ENUM_SEPARATOR_PATTERN = Regex("[\\s-]+")
