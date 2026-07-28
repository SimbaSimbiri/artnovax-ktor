package com.simbiri.presentation.utils.therapy

import com.simbiri.domain.model.therapy.TherapyGoal
import com.simbiri.domain.model.therapy.TherapyIntensity
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.routes.path.PublishedTherapyRoutesPath

/**
 * Parsed public catalogue filters supplied to
 * GetPublishedTherapySessionsUseCase.
 */
data class PublishedTherapyRouteFilters(
    val goal: TherapyGoal? = null,
    val intensity: TherapyIntensity? = null,
    val locale: String? = null,
)

/**
 * Converts public query-parameter strings into typed domain values.
 */
fun PublishedTherapyRoutesPath
        .toPublishedTherapyFiltersOrFailure(
    operation: String,
): ResultType<PublishedTherapyRouteFilters, DataError> {
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
        PublishedTherapyRouteFilters(
            goal = parsedGoal,
            intensity = parsedIntensity,
            locale = parsedLocale,
        )
    )
}
