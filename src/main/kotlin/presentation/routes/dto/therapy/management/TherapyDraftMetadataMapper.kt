package com.simbiri.presentation.routes.dto.therapy.management

import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.therapy.TherapeuticPriority
import com.simbiri.domain.model.therapy.TherapyContentStatus
import com.simbiri.domain.model.therapy.TherapyContraindication
import com.simbiri.domain.model.therapy.TherapyGoal
import com.simbiri.domain.model.therapy.TherapyIntensity
import com.simbiri.domain.model.therapy.TherapySession
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.util.Locale

/**
 * Converts an untrusted request into a new server-owned draft.
 */
fun TherapyDraftMetadataRequestDto.toNewTherapyDraftOrFailure(
    authenticatedUserId: UserId,
): ResultType<TherapySession, DataError> = when (val parsedMetadata = parseMetadataOrFailure()) {
    is ResultType.Success -> {
        val metadata = parsedMetadata.data

        ResultType.Success(
            TherapySession(
                id = null,
                seriesId = null,
                authorId = authenticatedUserId,
                title = metadata.title,
                description = metadata.description,
                intensity = metadata.intensity,
                locale = metadata.locale,
                tagline = metadata.tagline,
                status = TherapyContentStatus.DRAFT,
                version = 1,
                therapeuticPriority = metadata.therapeuticPriority,
                goalTags = metadata.goalTags,
                contraindications = metadata.contraindications,
                cultureTags = metadata.cultureTags,
                coverAsset = null,
                modules = emptyList(),
                createdAt = null,
                updatedAt = null,
                publishedAt = null,
                archivedAt = null,
            )
        )
    }

    is ResultType.Failure -> ResultType.Failure(
        parsedMetadata.error
    )
}

/**
 * Converts an untrusted request into an update candidate.
 *
 * UpdateTherapyDraftUseCase replaces all server-owned fields with persisted
 * values before validation and persistence.
 */
fun TherapyDraftMetadataRequestDto.toTherapyDraftUpdateOrFailure(
    authenticatedUserId: UserId,
    therapySessionId: TherapySessionId,
): ResultType<TherapySession, DataError> = when (val parsedMetadata = parseMetadataOrFailure()) {
    is ResultType.Success -> {
        val metadata = parsedMetadata.data

        ResultType.Success(
            TherapySession(
                id = therapySessionId,

                /*
                 * The use case replaces this placeholder with the
                 * persisted series ID.
                 */
                seriesId = null,

                /*
                 * The use case replaces this with the persisted author.
                 */
                authorId = authenticatedUserId,

                title = metadata.title,

                description = metadata.description,

                intensity = metadata.intensity,

                locale = metadata.locale,

                tagline = metadata.tagline,

                status = TherapyContentStatus.DRAFT,

                /*
                 * The use case replaces this with the persisted version.
                 */
                version = 1,

                therapeuticPriority = metadata.therapeuticPriority,

                goalTags = metadata.goalTags,

                contraindications = metadata.contraindications,

                cultureTags = metadata.cultureTags,

                coverAsset = null,
                modules = emptyList(),

                createdAt = null,
                updatedAt = null,
                publishedAt = null,
                archivedAt = null,
            )
        )
    }

    is ResultType.Failure -> ResultType.Failure(
        parsedMetadata.error
    )
}

/**
 * Parses and normalizes every editable metadata field.
 */
private fun TherapyDraftMetadataRequestDto.parseMetadataOrFailure(): ResultType<ParsedTherapyDraftMetadata, DataError> {
    val parsedPriority = when (val result = parseRequiredEnum<TherapeuticPriority>(
        field = "therapeuticPriority",
        rawValue = therapeuticPriority,
    )) {
        is ResultType.Success -> result.data

        is ResultType.Failure -> return ResultType.Failure(
            result.error
        )
    }

    val parsedIntensity = when (val result = parseRequiredEnum<TherapyIntensity>(
        field = "intensity",
        rawValue = intensity,
    )) {
        is ResultType.Success -> result.data

        is ResultType.Failure -> return ResultType.Failure(
            result.error
        )
    }

    val parsedGoals = when (val result = parseEnumSet<TherapyGoal>(
        field = "goalTags",
        rawValues = goalTags,
    )) {
        is ResultType.Success -> result.data

        is ResultType.Failure -> return ResultType.Failure(
            result.error
        )
    }

    val parsedContraindications = when (val result = parseEnumSet<TherapyContraindication>(
        field = "contraindications",
        rawValues = contraindications,
    )) {
        is ResultType.Success -> result.data

        is ResultType.Failure -> return ResultType.Failure(
            result.error
        )
    }

    val normalizedLocale = when (val result = normalizeLocaleOrFailure(
        locale
    )) {
        is ResultType.Success -> result.data

        is ResultType.Failure -> return ResultType.Failure(
            result.error
        )
    }

    val normalizedCultureTags = when (val result = normalizeCultureTagsOrFailure(
        cultureTags
    )) {
        is ResultType.Success -> result.data

        is ResultType.Failure -> return ResultType.Failure(
            result.error
        )
    }

    return ResultType.Success(
        ParsedTherapyDraftMetadata(
            title = title.trim(),

            description = description.trim(),
            tagline = tagline?.trim()?.takeIf(
                    String::isNotEmpty
                ),
            therapeuticPriority = parsedPriority,

            intensity = parsedIntensity,
            locale = normalizedLocale,

            goalTags = parsedGoals,
            contraindications = parsedContraindications,
            cultureTags = normalizedCultureTags,
        )
    )
}

/**
 * Parses enum values while accepting API-friendly spelling.
 *
 * Examples:
 * - MENTAL_HEALTH
 * - mental-health
 * - mental health
 */
private inline fun <
        reified E : Enum<E>,
        > parseRequiredEnum(
    field: String,
    rawValue: String,
): ResultType<E, DataError> {
    val normalizedValue = normalizeEnumToken(
        rawValue
    )

    if (normalizedValue.isEmpty()) {
        return validationFailure(
            field = field,
            value = rawValue,
            reason = "A nonblank value is required.",
        )
    }

    val parsedValue = enumValues<E>().firstOrNull { value ->
            value.name == normalizedValue
        }

    if (parsedValue != null) {
        return ResultType.Success(
            parsedValue
        )
    }

    val allowedValues = enumValues<E>().joinToString(
            separator = ", ",
        ) { value ->
            value.name
        }

    return validationFailure(
        field = field,
        value = rawValue,
        reason = "Unsupported value. Allowed values: $allowedValues.",
    )
}

/**
 * Parses a set of enums while rejecting duplicate normalized values.
 */
private inline fun <
        reified E : Enum<E>,
        > parseEnumSet(
    field: String,
    rawValues: List<String>,
): ResultType<Set<E>, DataError> {
    val parsedValues = linkedSetOf<E>()

    rawValues.forEachIndexed {
            index,
            rawValue,
        ->

        val parsedValue = when (val result = parseRequiredEnum<E>(
            field = "$field[$index]",

            rawValue = rawValue,
        )) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> return ResultType.Failure(
                result.error
            )
        }

        if (!parsedValues.add(
                parsedValue
            )
        ) {
            return validationFailure(
                field = "$field[$index]",

                value = rawValue,

                reason = "Duplicate value after normalization.",
            )
        }
    }

    return ResultType.Success(
        parsedValues
    )
}

/**
 * Validates and canonicalizes one locale.
 */
private fun normalizeLocaleOrFailure(
    rawLocale: String,
): ResultType<String, DataError> {
    val candidate = rawLocale.trim().replace(
            oldChar = '_',
            newChar = '-',
        )

    if (candidate.isEmpty() || !LOCALE_PATTERN.matches(candidate)) {
        return validationFailure(
            field = "locale",

            value = rawLocale,

            reason = "Locale must use a BCP-47-like value such as " + "'en', 'en-US', or 'sw-KE'.",
        )
    }

    val normalizedLocale = Locale.forLanguageTag(
            candidate
        ).toLanguageTag()

    if (normalizedLocale.equals(
            other = "und",
            ignoreCase = true,
        )
    ) {
        return validationFailure(
            field = "locale",

            value = rawLocale,

            reason = "Locale could not be recognized.",
        )
    }

    return ResultType.Success(
        normalizedLocale
    )
}

/**
 * Trims culture tags and rejects case-insensitive duplicates.
 */
private fun normalizeCultureTagsOrFailure(
    rawCultureTags: List<String>,
): ResultType<Set<String>, DataError> {
    val normalizedKeys = mutableSetOf<String>()

    val displayValues = linkedSetOf<String>()

    rawCultureTags.forEachIndexed {
            index,
            rawCultureTag,
        ->

        val displayValue = rawCultureTag.trim()

        if (displayValue.isEmpty()) {
            return validationFailure(
                field = "cultureTags[$index]",
                value = rawCultureTag,
                reason = "Culture tags must not be blank.",
            )
        }

        val normalizedKey = displayValue.lowercase(
            Locale.ROOT
        )

        if (!normalizedKeys.add(
                normalizedKey
            )
        ) {
            return validationFailure(
                field = "cultureTags[$index]",
                value = rawCultureTag,
                reason = "Duplicate culture tag after trimming and " + "case normalization.",
            )
        }

        displayValues += displayValue
    }

    return ResultType.Success(
        displayValues
    )
}

private fun normalizeEnumToken(
    rawValue: String,
): String = rawValue.trim().uppercase(
        Locale.ROOT
    ).replace(
        ENUM_SEPARATOR_PATTERN,
        "_",
    )

private fun <D> validationFailure(
    field: String,
    value: String,
    reason: String,
): ResultType<D, DataError> = ResultType.Failure(
    DataError.ValidationError(
        message = "Therapy draft metadata is invalid. field=$field, value='$value'. $reason"
    )
)

private data class ParsedTherapyDraftMetadata(
    val title: String,
    val description: String,
    val tagline: String?,

    val therapeuticPriority: TherapeuticPriority,
    val intensity: TherapyIntensity,
    val locale: String,

    val goalTags: Set<TherapyGoal>,
    val contraindications: Set<TherapyContraindication>,

    val cultureTags: Set<String>,
)

private val ENUM_SEPARATOR_PATTERN = Regex("[\\s-]+")

private val LOCALE_PATTERN = Regex(
    "^[A-Za-z]{2,8}" + "([_-][A-Za-z0-9]{1,8})*$"
)
