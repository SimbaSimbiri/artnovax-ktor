package com.simbiri.domain.policy.therapy

import com.simbiri.domain.model.therapy.TherapyAssetRole
import com.simbiri.domain.model.therapy.TherapyMediaType
import com.simbiri.domain.model.therapy.TherapyModality
import com.simbiri.domain.model.therapy.TherapyModule
import com.simbiri.domain.util.DataError

/**
 * Pure validation rules for one ordered TherapyModule.
 */
object TherapyModulePolicy {

    private const val MIN_TITLE_LENGTH = 3
    private const val MAX_TITLE_LENGTH = 150

    private const val MAX_GOAL_LENGTH = 500
    private const val MAX_INSTRUCTIONS_LENGTH = 10_000
    private const val MAX_WHY_THIS_HELPS_LENGTH = 2_000

    private const val MIN_PUBLISHED_DURATION_SECONDS = 10
    private const val MAX_DURATION_SECONDS = 3_600

    private const val MAX_ASSET_COUNT = 20

    /**
     * Draft modules may still be incomplete, but all supplied values
     * must be structurally safe.
     */
    fun validateDraft(
        module: TherapyModule,
    ): DataError.ValidationError? {
        val title = module.title.trim()
        val goal = module.goal.trim()
        val instructions = module.instructions.trim()
        val whyThisHelps = module.whyThisHelps.trim()

        return when {
            module.orderIndex < 0 -> {
                validationError(
                    field = "orderIndex",
                    value = module.orderIndex,
                    reason = "Module order index cannot be negative."
                )
            }

            title.length !in
                    MIN_TITLE_LENGTH..MAX_TITLE_LENGTH -> {
                validationError(
                    field = "title",
                    value = module.title,
                    reason = "Module title must contain between " +
                            "$MIN_TITLE_LENGTH and $MAX_TITLE_LENGTH " +
                            "characters."
                )
            }

            goal.length > MAX_GOAL_LENGTH -> {
                validationError(
                    field = "goal",
                    value = "length=${goal.length}",
                    reason = "Module goal cannot exceed " +
                            "$MAX_GOAL_LENGTH characters."
                )
            }

            instructions.length > MAX_INSTRUCTIONS_LENGTH -> {
                validationError(
                    field = "instructions",
                    value = "length=${instructions.length}",
                    reason = "Module instructions cannot exceed " +
                            "$MAX_INSTRUCTIONS_LENGTH characters."
                )
            }

            whyThisHelps.length >
                    MAX_WHY_THIS_HELPS_LENGTH -> {
                validationError(
                    field = "whyThisHelps",
                    value = "length=${whyThisHelps.length}",
                    reason = "The therapeutic explanation cannot exceed " +
                            "$MAX_WHY_THIS_HELPS_LENGTH characters."
                )
            }

            module.estimatedDurationSeconds < 0 -> {
                validationError(
                    field = "estimatedDurationSeconds",
                    value = module.estimatedDurationSeconds,
                    reason = "Draft module duration cannot be negative."
                )
            }

            module.estimatedDurationSeconds >
                    MAX_DURATION_SECONDS -> {
                validationError(
                    field = "estimatedDurationSeconds",
                    value = module.estimatedDurationSeconds,
                    reason = "A module cannot exceed " +
                            "$MAX_DURATION_SECONDS seconds."
                )
            }

            module.assets.size > MAX_ASSET_COUNT -> {
                validationError(
                    field = "assets",
                    value = module.assets.size,
                    reason = "A module cannot contain more than " +
                            "$MAX_ASSET_COUNT assets."
                )
            }

            module.assets.any { asset ->
                asset.role == TherapyAssetRole.SESSION_COVER
            } -> {
                val index =
                    module.assets.indexOfFirst { asset ->
                        asset.role ==
                                TherapyAssetRole.SESSION_COVER
                    }

                validationError(
                    field = "assets[$index].role",
                    value = module.assets[index].role,
                    reason = "Session-cover assets belong to the " +
                            "TherapySession, not an individual module."
                )
            }

            duplicateAssetId(module) != null -> {
                validationError(
                    field = "assets",
                    value = duplicateAssetId(module),
                    reason = "A module cannot contain the same persisted " +
                            "asset ID more than once."
                )
            }

            duplicateStorageKey(module) != null -> {
                validationError(
                    field = "assets",
                    value = duplicateStorageKey(module),
                    reason = "A module cannot reference the same asset " +
                            "storage key more than once."
                )
            }

            duplicateAssetRoleAndLocale(module) != null -> {
                validationError(
                    field = "assets",
                    value = duplicateAssetRoleAndLocale(module),
                    reason = "A module cannot contain duplicate asset " +
                            "roles for the same locale."
                )
            }

            else -> {
                validateAssetsForDraft(module)
            }
        }
    }

    /**
     * Publication requires complete instructions, duration, therapeutic
     * explanation, and any modality-specific media.
     */
    fun validateForPublication(
        module: TherapyModule,
    ): DataError.ValidationError? {
        validateDraft(module)?.let { error ->
            return error
        }

        val goal = module.goal.trim()
        val instructions = module.instructions.trim()
        val whyThisHelps = module.whyThisHelps.trim()

        return when {
            goal.isBlank() -> {
                validationError(
                    field = "goal",
                    value = module.goal,
                    reason = "Published modules require a clear goal."
                )
            }

            instructions.isBlank() -> {
                validationError(
                    field = "instructions",
                    value = module.instructions,
                    reason = "Published modules require user guidance."
                )
            }

            whyThisHelps.isBlank() -> {
                validationError(
                    field = "whyThisHelps",
                    value = module.whyThisHelps,
                    reason = "Published modules require a plain-language " +
                            "explanation of their therapeutic purpose."
                )
            }

            module.estimatedDurationSeconds <
                    MIN_PUBLISHED_DURATION_SECONDS -> {
                validationError(
                    field = "estimatedDurationSeconds",
                    value = module.estimatedDurationSeconds,
                    reason = "Published modules must last at least " +
                            "$MIN_PUBLISHED_DURATION_SECONDS seconds."
                )
            }

            module.modality == TherapyModality.GUIDED_AUDIO &&
                    !hasPrimaryMedia(
                        module = module,
                        mediaType = TherapyMediaType.AUDIO,
                    ) -> {
                validationError(
                    field = "assets",
                    value = module.assets.map { asset ->
                        asset.role to asset.mediaType
                    },
                    reason = "A GUIDED_AUDIO module requires a primary " +
                            "audio asset."
                )
            }

            module.modality == TherapyModality.GUIDED_VIDEO &&
                    !hasPrimaryMedia(
                        module = module,
                        mediaType = TherapyMediaType.VIDEO,
                    ) -> {
                validationError(
                    field = "assets",
                    value = module.assets.map { asset ->
                        asset.role to asset.mediaType
                    },
                    reason = "A GUIDED_VIDEO module requires a primary " +
                            "video asset."
                )
            }

            primaryAudioOrVideoNeedsTranscript(module) -> {
                validationError(
                    field = "assets",
                    value = module.assets.map { asset ->
                        asset.role to asset.mediaType
                    },
                    reason = "Primary audio and video guidance requires " +
                            "an inline transcript, caption asset, or " +
                            "transcript asset."
                )
            }

            else -> {
                validateAssetsForPublication(module)
            }
        }
    }

    private fun validateAssetsForDraft(
        module: TherapyModule,
    ): DataError.ValidationError? {
        module.assets.forEachIndexed { index, asset ->
            TherapyAssetPolicy
                .validateDraft(asset)
                ?.let { nestedError ->
                    return nestedValidationError(
                        index = index,
                        nestedError = nestedError,
                    )
                }
        }

        return null
    }

    private fun validateAssetsForPublication(
        module: TherapyModule,
    ): DataError.ValidationError? {
        module.assets.forEachIndexed { index, asset ->
            TherapyAssetPolicy
                .validateForPublication(asset)
                ?.let { nestedError ->
                    return nestedValidationError(
                        index = index,
                        nestedError = nestedError,
                    )
                }
        }

        return null
    }

    private fun hasPrimaryMedia(
        module: TherapyModule,
        mediaType: TherapyMediaType,
    ): Boolean =
        module.assets.any { asset ->
            asset.role == TherapyAssetRole.PRIMARY_MEDIA &&
                    asset.mediaType == mediaType
        }

    private fun primaryAudioOrVideoNeedsTranscript(
        module: TherapyModule,
    ): Boolean {
        val primaryAudioVideoAssets =
            module.assets.filter { asset ->
                asset.role == TherapyAssetRole.PRIMARY_MEDIA &&
                        asset.mediaType in setOf(
                    TherapyMediaType.AUDIO,
                    TherapyMediaType.VIDEO,
                )
            }

        if (primaryAudioVideoAssets.isEmpty()) {
            return false
        }

        val hasSeparateAccessibilityAsset =
            module.assets.any { asset ->
                asset.role in setOf(
                    TherapyAssetRole.CAPTION,
                    TherapyAssetRole.TRANSCRIPT,
                ) &&
                        asset.mediaType == TherapyMediaType.TEXT
            }

        if (hasSeparateAccessibilityAsset) {
            return false
        }

        return primaryAudioVideoAssets.any { asset ->
            asset.transcript.isNullOrBlank()
        }
    }

    private fun duplicateAssetId(
        module: TherapyModule,
    ): Any? =
        module.assets
            .mapNotNull { asset -> asset.id }
            .groupingBy { assetId -> assetId }
            .eachCount()
            .entries
            .firstOrNull { (_, count) -> count > 1 }
            ?.key

    private fun duplicateStorageKey(
        module: TherapyModule,
    ): String? =
        module.assets
            .groupingBy { asset ->
                asset.storageKey.trim().lowercase()
            }
            .eachCount()
            .entries
            .firstOrNull { (_, count) -> count > 1 }
            ?.key

    private fun duplicateAssetRoleAndLocale(
        module: TherapyModule,
    ): Pair<TherapyAssetRole, String?>? =
        module.assets
            .groupingBy { asset ->
                asset.role to
                        asset.locale
                            ?.trim()
                            ?.lowercase()
            }
            .eachCount()
            .entries
            .firstOrNull { (_, count) -> count > 1 }
            ?.key

    private fun nestedValidationError(
        index: Int,
        nestedError: DataError.ValidationError,
    ): DataError.ValidationError =
        DataError.ValidationError(
            message = "Therapy module validation failed. " +
                    "field=assets[$index]. " +
                    "Nested error: ${nestedError.message}"
        )

    private fun validationError(
        field: String,
        value: Any?,
        reason: String,
    ): DataError.ValidationError =
        DataError.ValidationError(
            message = "Therapy module validation failed. " +
                    "field=$field, value=$value. $reason"
        )
}
