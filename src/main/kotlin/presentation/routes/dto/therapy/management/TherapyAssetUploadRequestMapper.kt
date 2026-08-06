package com.simbiri.presentation.routes.dto.therapy.management

import com.simbiri.application.therapy.asset.TherapyAssetUploadRequest
import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.therapy.TherapyAssetRole
import com.simbiri.domain.model.therapy.TherapyMediaType
import com.simbiri.domain.storage.TherapyAssetUploadGrant
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.utils.therapy.parseTherapyModuleIdOrFailure
import java.util.Locale
import kotlin.enums.enumEntries

fun TherapyAssetUploadRequestDto.toTherapyAssetUploadRequestOrFailure(
    therapySessionId: TherapySessionId,
): ResultType<TherapyAssetUploadRequest, DataError> {
    val parsedRole = when (val result = parseEnumOrFailure<TherapyAssetRole>("role", role)) {
        is ResultType.Success -> result.data
        is ResultType.Failure -> return ResultType.Failure(result.error)
    }

    val parsedMediaType = when (val result = parseEnumOrFailure<TherapyMediaType>("mediaType", mediaType)) {
        is ResultType.Success -> result.data
        is ResultType.Failure -> return ResultType.Failure(result.error)
    }

    val parsedModuleId = when (val result = parseOptionalModuleIdOrFailure(therapyModuleId)) {
        is ResultType.Success -> result.data
        is ResultType.Failure -> return ResultType.Failure(result.error)
    }

    return ResultType.Success(
        TherapyAssetUploadRequest(
            therapySessionId = therapySessionId,
            therapyModuleId = parsedModuleId,
            role = parsedRole,
            mediaType = parsedMediaType,
            mimeType = mimeType.trim(),
            sizeBytes = sizeBytes,
            sha256 = sha256.trim(),
            locale = locale?.trim()?.takeIf(String::isNotEmpty),
            altText = altText?.trim()?.takeIf(String::isNotEmpty),
            transcript = transcript?.trim()?.takeIf(String::isNotEmpty),
        )
    )
}

fun TherapyAssetUploadGrant.toResponseDto(): TherapyAssetUploadGrantResponseDto =
    TherapyAssetUploadGrantResponseDto(
        uploadUrl = uploadUrl,
        storageKey = storageKey,
        expiresAt = expiresAt.toString(),
        requiredHeaders = requiredHeaders,
    )

private fun parseOptionalModuleIdOrFailure(
    rawTherapyModuleId: String?,
): ResultType<TherapyModuleId?, DataError> {
    if (rawTherapyModuleId == null) {
        return ResultType.Success(null)
    }

    return when (
        val result = parseTherapyModuleIdOrFailure(
            operation = "requestTherapyAssetUpload",
            field = "therapyModuleId",
            rawTherapyModuleId = rawTherapyModuleId,
        )
    ) {
        is ResultType.Success -> ResultType.Success(result.data)
        is ResultType.Failure -> ResultType.Failure(result.error)
    }
}

private inline fun <reified E : Enum<E>> parseEnumOrFailure(
    field: String,
    rawValue: String,
): ResultType<E, DataError> {
    val normalizedValue = rawValue.trim()
        .uppercase(Locale.ROOT)
        .replace(ENUM_SEPARATOR_PATTERN, "_")

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
            message = "Therapy asset upload request is invalid. field=$field, value='$rawValue'. " +
                    "Allowed values: $allowedValues."
        )
    )
}

private val ENUM_SEPARATOR_PATTERN = Regex("[\\s-]+")
