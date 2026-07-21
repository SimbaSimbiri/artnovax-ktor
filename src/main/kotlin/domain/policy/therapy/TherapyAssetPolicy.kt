package com.simbiri.domain.policy.therapy

import com.simbiri.domain.model.therapy.TherapyAsset
import com.simbiri.domain.model.therapy.TherapyAssetRole
import com.simbiri.domain.model.therapy.TherapyMediaType
import com.simbiri.domain.util.DataError

/**
 * Pure validation rules for therapy-content assets.
 *
 * This policy validates metadata only. It does not check whether an
 * object exists in S3 or generate signed URLs.
 */
object TherapyAssetPolicy {

    private const val MAX_STORAGE_KEY_LENGTH = 1_024
    private const val MAX_MIME_TYPE_LENGTH = 255
    private const val MAX_LOCALE_LENGTH = 35
    private const val MAX_ALT_TEXT_LENGTH = 1_000
    private const val MAX_TRANSCRIPT_LENGTH = 100_000

    private const val MAX_ASSET_SIZE_BYTES =
        1_073_741_824L

    private val SHA_256_REGEX =
        Regex("^[A-Fa-f0-9]{64}$")

    private val MIME_TYPE_REGEX =
        Regex(
            "^[A-Za-z0-9][A-Za-z0-9!#$&^_.+-]*/" +
                    "[A-Za-z0-9][A-Za-z0-9!#$&^_.+-]*$"
        )

    private val LOCALE_REGEX =
        Regex("^[A-Za-z]{2,8}(-[A-Za-z0-9]{1,8})*$")

    /**
     * Validates metadata that must be valid even while content is a
     * draft.
     */
    fun validateDraft(
        asset: TherapyAsset,
    ): DataError.ValidationError? {
        val storageKey = asset.storageKey.trim()
        val mimeType = asset.mimeType.trim()
        val locale = asset.locale?.trim()
        val altText = asset.altText?.trim()
        val transcript = asset.transcript?.trim()

        return when {
            storageKey.isBlank() -> {
                validationError(
                    field = "storageKey",
                    value = asset.storageKey,
                    reason = "Therapy asset storage key is required."
                )
            }

            storageKey.length > MAX_STORAGE_KEY_LENGTH -> {
                validationError(
                    field = "storageKey",
                    value = asset.storageKey,
                    reason = "Therapy asset storage key cannot exceed " +
                            "$MAX_STORAGE_KEY_LENGTH characters."
                )
            }

            storageKey.startsWith("/") -> {
                validationError(
                    field = "storageKey",
                    value = asset.storageKey,
                    reason = "Storage key must be relative and cannot " +
                            "start with '/'."
                )
            }

            storageKey.contains("\\") -> {
                validationError(
                    field = "storageKey",
                    value = asset.storageKey,
                    reason = "Storage key must use forward slashes."
                )
            }

            storageKey
                .split("/")
                .any { segment -> segment == ".." } -> {
                validationError(
                    field = "storageKey",
                    value = asset.storageKey,
                    reason = "Storage key cannot contain parent-directory " +
                            "segments."
                )
            }

            mimeType.isBlank() -> {
                validationError(
                    field = "mimeType",
                    value = asset.mimeType,
                    reason = "Therapy asset MIME type is required."
                )
            }

            mimeType.length > MAX_MIME_TYPE_LENGTH -> {
                validationError(
                    field = "mimeType",
                    value = asset.mimeType,
                    reason = "MIME type cannot exceed " +
                            "$MAX_MIME_TYPE_LENGTH characters."
                )
            }

            !MIME_TYPE_REGEX.matches(mimeType) -> {
                validationError(
                    field = "mimeType",
                    value = asset.mimeType,
                    reason = "Therapy asset MIME type has an invalid format."
                )
            }

            asset.sizeBytes <= 0L -> {
                validationError(
                    field = "sizeBytes",
                    value = asset.sizeBytes,
                    reason = "Therapy asset size must be greater than zero."
                )
            }

            asset.sizeBytes > MAX_ASSET_SIZE_BYTES -> {
                validationError(
                    field = "sizeBytes",
                    value = asset.sizeBytes,
                    reason = "A single therapy asset cannot exceed " +
                            "$MAX_ASSET_SIZE_BYTES bytes."
                )
            }

            !SHA_256_REGEX.matches(asset.sha256.trim()) -> {
                validationError(
                    field = "sha256",
                    value = asset.sha256,
                    reason = "SHA-256 must contain exactly 64 " +
                            "hexadecimal characters."
                )
            }

            locale != null &&
                    locale.length > MAX_LOCALE_LENGTH -> {
                validationError(
                    field = "locale",
                    value = asset.locale,
                    reason = "Asset locale cannot exceed " +
                            "$MAX_LOCALE_LENGTH characters."
                )
            }

            locale != null &&
                    !LOCALE_REGEX.matches(locale) -> {
                validationError(
                    field = "locale",
                    value = asset.locale,
                    reason = "Asset locale must use a valid BCP-47-like " +
                            "format, such as 'en', 'en-KE', or 'sw-KE'."
                )
            }

            altText != null &&
                    altText.length > MAX_ALT_TEXT_LENGTH -> {
                validationError(
                    field = "altText",
                    value = asset.altText,
                    reason = "Asset alternative text cannot exceed " +
                            "$MAX_ALT_TEXT_LENGTH characters."
                )
            }

            transcript != null &&
                    transcript.length > MAX_TRANSCRIPT_LENGTH -> {
                validationError(
                    field = "transcript",
                    value = "length=${transcript.length}",
                    reason = "Asset transcript cannot exceed " +
                            "$MAX_TRANSCRIPT_LENGTH characters."
                )
            }

            !roleMatchesMediaType(asset) -> {
                validationError(
                    field = "mediaType",
                    value = "role=${asset.role}, " +
                            "mediaType=${asset.mediaType}",
                    reason = "Asset media type is incompatible with its " +
                            "assigned role."
                )
            }

            else -> null
        }
    }

    /**
     * Adds accessibility requirements that must be satisfied before
     * content is published.
     */
    fun validateForPublication(
        asset: TherapyAsset,
    ): DataError.ValidationError? {
        validateDraft(asset)?.let { error ->
            return error
        }

        val requiresAlternativeText =
            asset.mediaType == TherapyMediaType.IMAGE &&
                    asset.role in setOf(
                TherapyAssetRole.SESSION_COVER,
                TherapyAssetRole.PRIMARY_MEDIA,
            )

        if (
            requiresAlternativeText &&
            asset.altText.isNullOrBlank()
        ) {
            return validationError(
                field = "altText",
                value = asset.altText,
                reason = "Published cover and primary image assets " +
                        "require alternative text."
            )
        }

        return null
    }

    private fun roleMatchesMediaType(
        asset: TherapyAsset,
    ): Boolean =
        when (asset.role) {
            TherapyAssetRole.SESSION_COVER,
            TherapyAssetRole.BACKGROUND_IMAGE ->
                asset.mediaType == TherapyMediaType.IMAGE

            TherapyAssetRole.BACKGROUND_AUDIO ->
                asset.mediaType == TherapyMediaType.AUDIO

            TherapyAssetRole.BACKGROUND_VIDEO ->
                asset.mediaType == TherapyMediaType.VIDEO

            TherapyAssetRole.CAPTION,
            TherapyAssetRole.TRANSCRIPT ->
                asset.mediaType == TherapyMediaType.TEXT

            TherapyAssetRole.PRIMARY_MEDIA ->
                true
        }

    private fun validationError(
        field: String,
        value: Any?,
        reason: String,
    ): DataError.ValidationError =
        DataError.ValidationError(
            message = "Therapy asset validation failed. " +
                    "field=$field, value=$value. $reason"
        )
}
