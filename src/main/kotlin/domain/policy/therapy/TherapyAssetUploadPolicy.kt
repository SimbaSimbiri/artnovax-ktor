package com.simbiri.domain.policy.therapy

import com.simbiri.application.therapy.asset.TherapyAssetUploadRequest
import com.simbiri.domain.model.therapy.TherapyAssetRole
import com.simbiri.domain.util.DataError
import java.util.Locale

object TherapyAssetUploadPolicy {
    private const val MAX_IMAGE_SIZE_BYTES = 25L * 1_024L * 1_024L
    private const val MAX_AUDIO_SIZE_BYTES = 150L * 1_024L * 1_024L
    private const val MAX_VIDEO_SIZE_BYTES = 750L * 1_024L * 1_024L
    private const val MAX_TEXT_SIZE_BYTES = 5L * 1_024L * 1_024L
    private const val MAX_FALLBACK_SIZE_BYTES = 750L * 1_024L * 1_024L
    private const val MAX_ALT_TEXT_LENGTH = 2_000
    private const val MAX_TRANSCRIPT_LENGTH = 100_000

    private val SHA_256_PATTERN = Regex("^[A-Fa-f0-9]{64}$")
    private val LOCALE_PATTERN = Regex("^[A-Za-z]{2,8}([_-][A-Za-z0-9]{1,8})*$")

    private val MIME_TYPES_BY_MEDIA_TYPE = mapOf(
        "IMAGE" to setOf("image/png", "image/jpeg", "image/webp"),
        "AUDIO" to setOf("audio/mpeg", "audio/mp4", "audio/wav", "audio/ogg"),
        "VIDEO" to setOf("video/mp4", "video/webm"),
        "TEXT" to setOf("text/plain", "text/vtt", "application/json"),
    )

    private val MAX_SIZE_BY_MEDIA_TYPE = mapOf(
        "IMAGE" to MAX_IMAGE_SIZE_BYTES,
        "AUDIO" to MAX_AUDIO_SIZE_BYTES,
        "VIDEO" to MAX_VIDEO_SIZE_BYTES,
        "TEXT" to MAX_TEXT_SIZE_BYTES,
    )

    fun validate(request: TherapyAssetUploadRequest): DataError.ValidationError? {
        val normalizedMimeType = request.mimeType.trim().lowercase(Locale.ROOT)
        val allowedMimeTypes = MIME_TYPES_BY_MEDIA_TYPE[request.mediaType.name]
        val maximumSize = MAX_SIZE_BY_MEDIA_TYPE[request.mediaType.name] ?: MAX_FALLBACK_SIZE_BYTES

        return when {
            request.role == TherapyAssetRole.SESSION_COVER && request.therapyModuleId != null -> {
                validationError(
                    field = "therapyModuleId",
                    value = request.therapyModuleId.value,
                    reason = "A session-cover asset must belong directly to the therapy session.",
                )
            }

            request.role != TherapyAssetRole.SESSION_COVER && request.therapyModuleId == null -> {
                validationError(
                    field = "therapyModuleId",
                    value = null,
                    reason = "A non-cover therapy asset must belong to a therapy module.",
                )
            }

            request.role == TherapyAssetRole.SESSION_COVER && request.mediaType.name != "IMAGE" -> {
                validationError(
                    field = "mediaType",
                    value = request.mediaType,
                    reason = "A session-cover asset must use IMAGE media.",
                )
            }

            normalizedMimeType.isEmpty() -> {
                validationError(
                    field = "mimeType",
                    value = request.mimeType,
                    reason = "A MIME type is required.",
                )
            }

            allowedMimeTypes == null || normalizedMimeType !in allowedMimeTypes -> {
                validationError(
                    field = "mimeType",
                    value = request.mimeType,
                    reason = "The MIME type is not supported for ${request.mediaType}.",
                )
            }

            request.sizeBytes <= 0L -> {
                validationError(
                    field = "sizeBytes",
                    value = request.sizeBytes,
                    reason = "Asset size must be greater than zero.",
                )
            }

            request.sizeBytes > maximumSize -> {
                validationError(
                    field = "sizeBytes",
                    value = request.sizeBytes,
                    reason = "The maximum size for ${request.mediaType} is $maximumSize bytes.",
                )
            }

            !SHA_256_PATTERN.matches(request.sha256.trim()) -> {
                validationError(
                    field = "sha256",
                    value = request.sha256,
                    reason = "SHA-256 must contain exactly 64 hexadecimal characters.",
                )
            }

            request.locale != null && !LOCALE_PATTERN.matches(request.locale.trim()) -> {
                validationError(
                    field = "locale",
                    value = request.locale,
                    reason = "Locale must use a BCP-47-like value such as 'en', 'en-US', or 'sw-KE'.",
                )
            }

            request.altText != null && request.altText.trim().length > MAX_ALT_TEXT_LENGTH -> {
                validationError(
                    field = "altText",
                    value = "length=${request.altText.trim().length}",
                    reason = "Alternative text cannot exceed $MAX_ALT_TEXT_LENGTH characters.",
                )
            }

            request.transcript != null && request.transcript.trim().length > MAX_TRANSCRIPT_LENGTH -> {
                validationError(
                    field = "transcript",
                    value = "length=${request.transcript.trim().length}",
                    reason = "Transcript text cannot exceed $MAX_TRANSCRIPT_LENGTH characters.",
                )
            }

            else -> null
        }
    }

    private fun validationError(
        field: String,
        value: Any?,
        reason: String,
    ): DataError.ValidationError = DataError.ValidationError(
        message = "Therapy asset upload request is invalid. field=$field, value=$value. $reason"
    )
}
