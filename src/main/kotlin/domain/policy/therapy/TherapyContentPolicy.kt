package com.simbiri.domain.policy.therapy

import com.simbiri.domain.model.therapy.TherapyAsset
import com.simbiri.domain.model.therapy.TherapyAssetRole
import com.simbiri.domain.model.therapy.TherapyContentStatus
import com.simbiri.domain.model.therapy.TherapySession
import com.simbiri.domain.util.DataError

/**
 * Pure business rules for authored TherapySession aggregates.
 *
 * This policy:
 * - does not know about HTTP DTOs;
 * - does not query users or object storage;
 * - does not depend on Ktor or Exposed;
 * - validates the aggregate as one unit.
 */
object TherapyContentPolicy {

    private const val MIN_TITLE_LENGTH = 3
    private const val MAX_TITLE_LENGTH = 150

    private const val MAX_DESCRIPTION_LENGTH = 5_000
    private const val MAX_TAGLINE_LENGTH = 255

    private const val MAX_LOCALE_LENGTH = 35
    private const val MAX_CULTURE_TAG_LENGTH = 80
    private const val MAX_CULTURE_TAG_COUNT = 20

    private const val MIN_PUBLISHED_MODULE_COUNT = 3
    private const val MAX_PUBLISHED_MODULE_COUNT = 7

    private const val MIN_PUBLISHED_DURATION_SECONDS = 60
    private const val MAX_PUBLISHED_DURATION_SECONDS = 7_200

    private val LOCALE_REGEX =
        Regex("^[A-Za-z]{2,8}(-[A-Za-z0-9]{1,8})*$")

    /**
     * Validates an editable draft.
     *
     * Drafts may still have incomplete descriptions, goals, and module
     * instructions.
     */
    fun validateDraft(
        session: TherapySession,
    ): DataError.ValidationError? {
        validateExpectedStatus(
            session = session,
            expectedStatus = TherapyContentStatus.DRAFT,
            operation = "draft validation",
        )?.let { error ->
            return error
        }

        validateSessionShape(session)?.let { error ->
            return error
        }

        validateDraftLifecycleMetadata(session)?.let { error ->
            return error
        }

        validateModuleCollectionForDraft(session)?.let { error ->
            return error
        }

        validateAggregateAssetUniqueness(session)?.let { error ->
            return error
        }

        return null
    }

    /**
     * Validates that a DRAFT is complete enough to enter review.
     */
    fun validateForReview(
        session: TherapySession,
    ): DataError.ValidationError? {
        validateDraft(session)?.let { error ->
            return error
        }

        return validatePublishableContent(session)
    }

    /**
     * Validates that reviewed content is complete enough to publish.
     *
     * Authorization and transition validation are handled separately.
     */
    fun validateForPublication(
        session: TherapySession,
    ): DataError.ValidationError? {
        validateExpectedStatus(
            session = session,
            expectedStatus = TherapyContentStatus.IN_REVIEW,
            operation = "publication validation",
        )?.let { error ->
            return error
        }

        validateSessionShape(session)?.let { error ->
            return error
        }

        validateAggregateAssetUniqueness(session)?.let { error ->
            return error
        }

        return validatePublishableContent(session)
    }

    private fun validateSessionShape(
        session: TherapySession,
    ): DataError.ValidationError? {
        val title = session.title.trim()
        val description = session.description.trim()
        val tagline = session.tagline?.trim()
        val locale = session.locale.trim()

        val blankCultureTagIndex =
            session.cultureTags
                .toList()
                .indexOfFirst { cultureTag ->
                    cultureTag.isBlank()
                }

        val longCultureTagIndex =
            session.cultureTags
                .toList()
                .indexOfFirst { cultureTag ->
                    cultureTag.trim().length >
                            MAX_CULTURE_TAG_LENGTH
                }

        val duplicateCultureTag =
            session.cultureTags
                .groupingBy { cultureTag ->
                    cultureTag.trim().lowercase()
                }
                .eachCount()
                .entries
                .firstOrNull { (_, count) ->
                    count > 1
                }
                ?.key

        return when {
            title.length !in
                    MIN_TITLE_LENGTH..MAX_TITLE_LENGTH -> {
                validationError(
                    field = "title",
                    value = session.title,
                    reason = "Therapy session title must contain " +
                            "between $MIN_TITLE_LENGTH and " +
                            "$MAX_TITLE_LENGTH characters."
                )
            }

            description.length > MAX_DESCRIPTION_LENGTH -> {
                validationError(
                    field = "description",
                    value = "length=${description.length}",
                    reason = "Therapy session description cannot exceed " +
                            "$MAX_DESCRIPTION_LENGTH characters."
                )
            }

            tagline != null &&
                    tagline.isBlank() -> {
                validationError(
                    field = "tagline",
                    value = session.tagline,
                    reason = "Tagline must be omitted rather than supplied " +
                            "as a blank value."
                )
            }

            tagline != null &&
                    tagline.length > MAX_TAGLINE_LENGTH -> {
                validationError(
                    field = "tagline",
                    value = session.tagline,
                    reason = "Therapy session tagline cannot exceed " +
                            "$MAX_TAGLINE_LENGTH characters."
                )
            }

            locale.isBlank() -> {
                validationError(
                    field = "locale",
                    value = session.locale,
                    reason = "Therapy session locale is required."
                )
            }

            locale.length > MAX_LOCALE_LENGTH -> {
                validationError(
                    field = "locale",
                    value = session.locale,
                    reason = "Therapy session locale cannot exceed " +
                            "$MAX_LOCALE_LENGTH characters."
                )
            }

            !LOCALE_REGEX.matches(locale) -> {
                validationError(
                    field = "locale",
                    value = session.locale,
                    reason = "Therapy session locale must use a valid " +
                            "BCP-47-like format."
                )
            }

            session.version <= 0 -> {
                validationError(
                    field = "version",
                    value = session.version,
                    reason = "Therapy content version must be positive."
                )
            }

            session.id != null &&
                    session.seriesId == null -> {
                validationError(
                    field = "seriesId",
                    value = session.seriesId,
                    reason = "A persisted therapy session must belong to a " +
                            "therapy-session series."
                )
            }

            session.version > 1 &&
                    session.seriesId == null -> {
                validationError(
                    field = "seriesId",
                    value = session.seriesId,
                    reason = "A therapy-session version greater than one must " +
                            "reference an existing therapy-session series."
                )
            }

            session.cultureTags.size >
                    MAX_CULTURE_TAG_COUNT -> {
                validationError(
                    field = "cultureTags",
                    value = session.cultureTags.size,
                    reason = "Therapy content cannot contain more than " +
                            "$MAX_CULTURE_TAG_COUNT culture tags."
                )
            }

            blankCultureTagIndex >= 0 -> {
                validationError(
                    field = "cultureTags[$blankCultureTagIndex]",
                    value = session
                        .cultureTags
                        .toList()[blankCultureTagIndex],
                    reason = "Culture tags cannot be blank."
                )
            }

            longCultureTagIndex >= 0 -> {
                validationError(
                    field = "cultureTags[$longCultureTagIndex]",
                    value = session
                        .cultureTags
                        .toList()[longCultureTagIndex],
                    reason = "Culture tags cannot exceed " +
                            "$MAX_CULTURE_TAG_LENGTH characters."
                )
            }

            duplicateCultureTag != null -> {
                validationError(
                    field = "cultureTags",
                    value = duplicateCultureTag,
                    reason = "Culture tags must be unique when compared " +
                            "case-insensitively."
                )
            }

            session.coverAsset != null &&
                    session.coverAsset.role !=
                    TherapyAssetRole.SESSION_COVER -> {
                validationError(
                    field = "coverAsset.role",
                    value = session.coverAsset.role,
                    reason = "The session cover asset must use the " +
                            "SESSION_COVER role."
                )
            }

            else -> {
                validateCoverAssetForDraft(session.coverAsset)
            }
        }
    }

    private fun validateDraftLifecycleMetadata(
        session: TherapySession,
    ): DataError.ValidationError? =
        when {
            session.publishedAt != null -> {
                validationError(
                    field = "publishedAt",
                    value = session.publishedAt,
                    reason = "Draft therapy content cannot have a " +
                            "publication timestamp."
                )
            }

            session.archivedAt != null -> {
                validationError(
                    field = "archivedAt",
                    value = session.archivedAt,
                    reason = "Draft therapy content cannot have an " +
                            "archive timestamp."
                )
            }

            else -> null
        }

    private fun validateModuleCollectionForDraft(
        session: TherapySession,
    ): DataError.ValidationError? {
        val duplicateOrderIndex =
            session.modules
                .groupingBy { module ->
                    module.orderIndex
                }
                .eachCount()
                .entries
                .firstOrNull { (_, count) ->
                    count > 1
                }
                ?.key

        if (duplicateOrderIndex != null) {
            return validationError(
                field = "modules",
                value = "orderIndex=$duplicateOrderIndex",
                reason = "Module order indices must be unique."
            )
        }

        val duplicateModuleId =
            session.modules
                .mapNotNull { module -> module.id }
                .groupingBy { moduleId -> moduleId }
                .eachCount()
                .entries
                .firstOrNull { (_, count) ->
                    count > 1
                }
                ?.key

        if (duplicateModuleId != null) {
            return validationError(
                field = "modules",
                value = "moduleId=$duplicateModuleId",
                reason = "A therapy session cannot contain the same " +
                        "persisted module ID more than once."
            )
        }

        session.modules.forEachIndexed { index, module ->
            TherapyModulePolicy
                .validateDraft(module)
                ?.let { nestedError ->
                    return nestedModuleValidationError(
                        index = index,
                        nestedError = nestedError,
                    )
                }
        }

        return null
    }

    private fun validatePublishableContent(
        session: TherapySession,
    ): DataError.ValidationError? {
        val description = session.description.trim()

        if (description.isBlank()) {
            return validationError(
                field = "description",
                value = session.description,
                reason = "Therapy content requires a description before " +
                        "it can be reviewed or published."
            )
        }

        if (
            session.modules.size !in
            MIN_PUBLISHED_MODULE_COUNT..
            MAX_PUBLISHED_MODULE_COUNT
        ) {
            return validationError(
                field = "modules",
                value = session.modules.size,
                reason = "Publishable therapy sessions require between " +
                        "$MIN_PUBLISHED_MODULE_COUNT and " +
                        "$MAX_PUBLISHED_MODULE_COUNT modules."
            )
        }

        if (session.goalTags.isEmpty()) {
            return validationError(
                field = "goalTags",
                value = session.goalTags,
                reason = "Publishable therapy content must declare at " +
                        "least one therapeutic goal."
            )
        }

        val expectedOrder =
            session.modules.indices.toList()

        val actualOrder =
            session.modules.map { module ->
                module.orderIndex
            }

        if (actualOrder != expectedOrder) {
            return validationError(
                field = "modules.orderIndex",
                value = actualOrder,
                reason = "Published modules must be sorted and use " +
                        "contiguous zero-based indices. " +
                        "expected=$expectedOrder."
            )
        }

        if (
            session.estimatedDurationSeconds !in
            MIN_PUBLISHED_DURATION_SECONDS..
            MAX_PUBLISHED_DURATION_SECONDS
        ) {
            return validationError(
                field = "estimatedDurationSeconds",
                value = session.estimatedDurationSeconds,
                reason = "Publishable therapy sessions must have a total " +
                        "estimated duration between " +
                        "$MIN_PUBLISHED_DURATION_SECONDS and " +
                        "$MAX_PUBLISHED_DURATION_SECONDS seconds."
            )
        }

        session.coverAsset?.let { coverAsset ->
            TherapyAssetPolicy
                .validateForPublication(coverAsset)
                ?.let { nestedError ->
                    return DataError.ValidationError(
                        message = "Therapy content validation failed. " +
                                "field=coverAsset. " +
                                "Nested error: ${nestedError.message}"
                    )
                }
        }

        session.modules.forEachIndexed { index, module ->
            TherapyModulePolicy
                .validateForPublication(module)
                ?.let { nestedError ->
                    return nestedModuleValidationError(
                        index = index,
                        nestedError = nestedError,
                    )
                }
        }

        return null
    }

    private fun validateAggregateAssetUniqueness(
        session: TherapySession,
    ): DataError.ValidationError? {
        val allAssets =
            buildList {
                session.coverAsset?.let(::add)

                session.modules.forEach { module ->
                    addAll(module.assets)
                }
            }

        val duplicateAssetId =
            allAssets
                .mapNotNull { asset -> asset.id }
                .groupingBy { assetId -> assetId }
                .eachCount()
                .entries
                .firstOrNull { (_, count) ->
                    count > 1
                }
                ?.key

        if (duplicateAssetId != null) {
            return validationError(
                field = "assets",
                value = "assetId=$duplicateAssetId",
                reason = "A TherapySession aggregate cannot contain the " +
                        "same persisted asset ID more than once."
            )
        }

        val duplicateStorageKey =
            allAssets
                .groupingBy { asset ->
                    asset.storageKey.trim().lowercase()
                }
                .eachCount()
                .entries
                .firstOrNull { (_, count) ->
                    count > 1
                }
                ?.key

        if (duplicateStorageKey != null) {
            return validationError(
                field = "assets",
                value = "storageKey=$duplicateStorageKey",
                reason = "A TherapySession aggregate cannot reference " +
                        "the same storage key more than once."
            )
        }

        return null
    }

    private fun validateCoverAssetForDraft(
        coverAsset: TherapyAsset?,
    ): DataError.ValidationError? {
        if (coverAsset == null) {
            return null
        }

        return TherapyAssetPolicy
            .validateDraft(coverAsset)
            ?.let { nestedError ->
                DataError.ValidationError(
                    message = "Therapy content validation failed. " +
                            "field=coverAsset. " +
                            "Nested error: ${nestedError.message}"
                )
            }
    }

    private fun validateExpectedStatus(
        session: TherapySession,
        expectedStatus: TherapyContentStatus,
        operation: String,
    ): DataError.ValidationError? {
        if (session.status == expectedStatus) {
            return null
        }

        return validationError(
            field = "status",
            value = session.status,
            reason = "Therapy content must have status " +
                    "$expectedStatus for $operation."
        )
    }

    private fun nestedModuleValidationError(
        index: Int,
        nestedError: DataError.ValidationError,
    ): DataError.ValidationError =
        DataError.ValidationError(
            message = "Therapy content validation failed. " +
                    "field=modules[$index]. " +
                    "Nested error: ${nestedError.message}"
        )

    private fun validationError(
        field: String,
        value: Any?,
        reason: String,
    ): DataError.ValidationError =
        DataError.ValidationError(
            message = "Therapy content validation failed. " +
                    "field=$field, value=$value. $reason"
        )
}
