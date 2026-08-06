package com.simbiri.presentation.routes.dto.therapy.management

import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.therapy.TherapyModality
import com.simbiri.domain.model.therapy.TherapyModule
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.utils.therapy.parseTherapyModuleIdOrFailure
import java.util.Locale
import kotlin.enums.enumEntries

fun CreateTherapyModuleRequestDto.toTherapyModuleOrFailure(): ResultType<TherapyModule, DataError> {
    val parsedModality = when (val result = parseTherapyModalityOrFailure(modality)) {
        is ResultType.Success -> result.data
        is ResultType.Failure -> return ResultType.Failure(result.error)
    }

    return ResultType.Success(
        TherapyModule(
            id = null,
            orderIndex = orderIndex,
            title = title.trim(),
            goal = goal.trim(),
            instructions = instructions.trim(),
            whyThisHelps = whyThisHelps.trim(),
            modality = parsedModality,
            estimatedDurationSeconds = estimatedDurationSeconds,
            isSkippable = isSkippable,
            isRepeatable = isRepeatable,
            assets = emptyList(),
            createdAt = null,
            updatedAt = null,
        )
    )
}

fun UpdateTherapyModuleRequestDto.toTherapyModuleOrFailure(
    therapyModuleId: TherapyModuleId,
): ResultType<TherapyModule, DataError> {
    val parsedModality = when (val result = parseTherapyModalityOrFailure(modality)) {
        is ResultType.Success -> result.data
        is ResultType.Failure -> return ResultType.Failure(result.error)
    }

    /*
     * orderIndex, assets, and timestamps are placeholders. UpdateTherapyModuleUseCase replaces them with persisted
     * server-owned values.
     */
    return ResultType.Success(
        TherapyModule(
            id = therapyModuleId,
            orderIndex = 0,
            title = title.trim(),
            goal = goal.trim(),
            instructions = instructions.trim(),
            whyThisHelps = whyThisHelps.trim(),
            modality = parsedModality,
            estimatedDurationSeconds = estimatedDurationSeconds,
            isSkippable = isSkippable,
            isRepeatable = isRepeatable,
            assets = emptyList(),
            createdAt = null,
            updatedAt = null,
        )
    )
}

fun ReorderTherapyModulesRequestDto.toTherapyModuleIdsOrFailure():
        ResultType<List<TherapyModuleId>, DataError> {
    val parsedIds = mutableListOf<TherapyModuleId>()

    orderedModuleIds.forEachIndexed { index, rawTherapyModuleId ->
        val therapyModuleId = when (
            val result = parseTherapyModuleIdOrFailure(
                operation = "reorderTherapyModules",
                field = "orderedModuleIds[$index]",
                rawTherapyModuleId = rawTherapyModuleId,
            )
        ) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> return ResultType.Failure(result.error)
        }

        parsedIds += therapyModuleId
    }

    return ResultType.Success(parsedIds)
}

private fun parseTherapyModalityOrFailure(
    rawModality: String,
): ResultType<TherapyModality, DataError> {
    val normalizedModality = rawModality.trim()
        .uppercase(Locale.ROOT)
        .replace(ENUM_SEPARATOR_PATTERN, "_")

    val modality = enumEntries<TherapyModality>().firstOrNull { value ->
        value.name == normalizedModality
    }

    if (modality != null) {
        return ResultType.Success(modality)
    }

    val allowedValues = enumEntries<TherapyModality>().joinToString(separator = ", ") { value ->
        value.name
    }

    return ResultType.Failure(
        DataError.ValidationError(
            message = "Therapy module request is invalid. field=modality, value='$rawModality'. " +
                    "Allowed values: $allowedValues."
        )
    )
}

private val ENUM_SEPARATOR_PATTERN = Regex("[\\s-]+")
