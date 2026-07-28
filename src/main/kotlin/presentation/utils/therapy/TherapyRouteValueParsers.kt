package com.simbiri.presentation.utils.therapy

import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.TherapySessionSeriesId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.util.Locale
import java.util.UUID

/**
 * Parses a required TherapySession identifier from an HTTP path value.
 */
fun parseTherapySessionIdOrFailure(
    operation: String,
    rawTherapySessionId: String?,
): ResultType<TherapySessionId, DataError> =
    when (
        val parsed = parseRequiredUuidOrFailure(
            operation = operation,
            field = "therapySessionId",
            rawValue = rawTherapySessionId,
        )
    ) {
        is ResultType.Success ->
            ResultType.Success(
                TherapySessionId(parsed.data)
            )

        is ResultType.Failure ->
            ResultType.Failure(parsed.error)
    }

/**
 * Parses a required therapy-session series identifier.
 */
fun parseTherapySessionSeriesIdOrFailure(
    operation: String,
    rawSeriesId: String?,
): ResultType<TherapySessionSeriesId, DataError> =
    when (
        val parsed = parseRequiredUuidOrFailure(
            operation = operation,
            field = "seriesId",
            rawValue = rawSeriesId,
        )
    ) {
        is ResultType.Success ->
            ResultType.Success(
                TherapySessionSeriesId(parsed.data)
            )

        is ResultType.Failure ->
            ResultType.Failure(parsed.error)
    }

/**
 * Parses an optional author filter.
 *
 * A missing value means that no author filter was requested. A supplied
 * blank or malformed value is rejected rather than treated as missing.
 */
fun parseOptionalTherapyAuthorIdOrFailure(
    operation: String,
    rawAuthorId: String?,
): ResultType<UserId?, DataError> =
    when (
        val parsed = parseOptionalUuidOrFailure(
            operation = operation,
            field = "authorId",
            rawValue = rawAuthorId,
        )
    ) {
        is ResultType.Success ->
            ResultType.Success(
                parsed.data?.let(::UserId)
            )

        is ResultType.Failure ->
            ResultType.Failure(parsed.error)
    }

/**
 * Parses and normalizes an optional BCP-47 locale.
 *
 * Examples:
 * - en
 * - en-US
 * - sw-KE
 */
fun parseOptionalTherapyLocaleOrFailure(
    operation: String,
    rawLocale: String?,
): ResultType<String?, DataError> {
    if (rawLocale == null) {
        return ResultType.Success(null)
    }

    val trimmedLocale = rawLocale.trim()

    if (trimmedLocale.isEmpty()) {
        return ResultType.Failure(
            routeValidationError(
                operation = operation,
                field = "locale",
                rawValue = rawLocale,
                reason = "A supplied locale must not be blank."
            )
        )
    }

    val normalizedLocale =
        Locale
            .forLanguageTag(trimmedLocale)
            .toLanguageTag()

    if (
        normalizedLocale.equals(
            other = "und",
            ignoreCase = true,
        )
    ) {
        return ResultType.Failure(
            routeValidationError(
                operation = operation,
                field = "locale",
                rawValue = rawLocale,
                reason = "Locale must be a valid BCP-47 language tag, " +
                        "such as 'en', 'en-US', or 'sw-KE'."
            )
        )
    }

    if (normalizedLocale.length > MAX_LOCALE_LENGTH) {
        return ResultType.Failure(
            routeValidationError(
                operation = operation,
                field = "locale",
                rawValue = rawLocale,
                reason = "Normalized locale must not exceed " +
                        "$MAX_LOCALE_LENGTH characters."
            )
        )
    }

    return ResultType.Success(normalizedLocale)
}

/**
 * Parses an optional enum query value.
 *
 * Parsing is case-insensitive. Hyphens and spaces are normalized to
 * underscores, allowing both `IN_REVIEW` and `in-review`.
 */
internal inline fun <reified E : Enum<E>>
        parseOptionalEnumOrFailure(
    operation: String,
    field: String,
    rawValue: String?,
): ResultType<E?, DataError> {
    if (rawValue == null) {
        return ResultType.Success(null)
    }

    val trimmedValue = rawValue.trim()

    if (trimmedValue.isEmpty()) {
        return ResultType.Failure(
            routeValidationError(
                operation = operation,
                field = field,
                rawValue = rawValue,
                reason = "A supplied value must not be blank."
            )
        )
    }

    val normalizedValue =
        trimmedValue
            .uppercase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_')

    val parsedValue =
        enumValues<E>()
            .firstOrNull { enumValue ->
                enumValue.name == normalizedValue
            }

    if (parsedValue != null) {
        return ResultType.Success(parsedValue)
    }

    val allowedValues =
        enumValues<E>()
            .joinToString(
                separator = ", ",
            ) { enumValue ->
                enumValue.name
            }

    return ResultType.Failure(
        routeValidationError(
            operation = operation,
            field = field,
            rawValue = rawValue,
            reason = "Unsupported value. Allowed values: " +
                    "$allowedValues."
        )
    )
}

/**
 * Parses a required UUID while preserving route-specific error context.
 */
private fun parseRequiredUuidOrFailure(
    operation: String,
    field: String,
    rawValue: String?,
): ResultType<UUID, DataError> {
    val trimmedValue = rawValue?.trim()

    if (trimmedValue.isNullOrEmpty()) {
        return ResultType.Failure(
            routeValidationError(
                operation = operation,
                field = field,
                rawValue = rawValue,
                reason = "A nonblank UUID is required."
            )
        )
    }

    val parsedUuid =
        runCatching {
            UUID.fromString(trimmedValue)
        }.getOrNull()

    return if (parsedUuid == null) {
        ResultType.Failure(
            routeValidationError(
                operation = operation,
                field = field,
                rawValue = rawValue,
                reason = "Value must be a valid UUID."
            )
        )
    } else {
        ResultType.Success(parsedUuid)
    }
}

/**
 * Parses an optional UUID.
 *
 * Null means the filter was omitted. Blank strings are considered invalid
 * client input.
 */
private fun parseOptionalUuidOrFailure(
    operation: String,
    field: String,
    rawValue: String?,
): ResultType<UUID?, DataError> {
    if (rawValue == null) {
        return ResultType.Success(null)
    }

    return when (
        val parsed = parseRequiredUuidOrFailure(
            operation = operation,
            field = field,
            rawValue = rawValue,
        )
    ) {
        is ResultType.Success ->
            ResultType.Success(parsed.data)

        is ResultType.Failure ->
            ResultType.Failure(parsed.error)
    }
}

/**
 * Produces presentation-layer validation errors without depending on data
 * repository utilities.
 */
internal fun routeValidationError(
    operation: String,
    field: String,
    rawValue: String?,
    reason: String,
): DataError.ValidationError =
    DataError.ValidationError(
        message = "$operation failed. " +
                "field=$field, " +
                "value=${rawValue ?: "null"}. " +
                reason
    )

private const val MAX_LOCALE_LENGTH = 35
