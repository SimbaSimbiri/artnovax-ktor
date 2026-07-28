package com.simbiri.presentation.utils.therapy

import com.simbiri.application.therapy.query.ManagedTherapyContentFilters
import com.simbiri.domain.model.therapy.TherapyContentStatus
import com.simbiri.domain.model.therapy.TherapyGoal
import com.simbiri.domain.model.therapy.TherapyIntensity
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.routes.path.ManagedTherapyRoutesPath

/**
 * Converts therapy-management query parameters into application-layer
 * filters.
 */
fun ManagedTherapyRoutesPath
        .toManagedTherapyFiltersOrFailure(
    operation: String,
): ResultType<ManagedTherapyContentFilters, DataError> {
    val parsedStatus =
        when (
            val result =
                parseOptionalEnumOrFailure<TherapyContentStatus>(
                    operation = operation,
                    field = "status",
                    rawValue = status,
                )
        ) {
            is ResultType.Success ->
                result.data

            is ResultType.Failure ->
                return ResultType.Failure(result.error)
        }

    val parsedAuthorId =
        when (
            val result =
                parseOptionalTherapyAuthorIdOrFailure(
                    operation = operation,
                    rawAuthorId = authorId,
                )
        ) {
            is ResultType.Success ->
                result.data

            is ResultType.Failure ->
                return ResultType.Failure(result.error)
        }

    val parsedGoal =
        when (
            val result =
                parseOptionalEnumOrFailure<TherapyGoal>(
                    operation = operation,
                    field = "goal",
                    rawValue = goal,
                )
        ) {
            is ResultType.Success ->
                result.data

            is ResultType.Failure ->
                return ResultType.Failure(result.error)
        }

    val parsedIntensity =
        when (
            val result =
                parseOptionalEnumOrFailure<TherapyIntensity>(
                    operation = operation,
                    field = "intensity",
                    rawValue = intensity,
                )
        ) {
            is ResultType.Success ->
                result.data

            is ResultType.Failure ->
                return ResultType.Failure(result.error)
        }

    val parsedLocale =
        when (
            val result =
                parseOptionalTherapyLocaleOrFailure(
                    operation = operation,
                    rawLocale = locale,
                )
        ) {
            is ResultType.Success ->
                result.data

            is ResultType.Failure ->
                return ResultType.Failure(result.error)
        }

    return ResultType.Success(
        ManagedTherapyContentFilters(
            status = parsedStatus,
            authorId = parsedAuthorId,
            goal = parsedGoal,
            intensity = parsedIntensity,
            locale = parsedLocale,
        )
    )
}
