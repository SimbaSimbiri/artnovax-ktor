package com.simbiri.domain.model.therapy

import com.simbiri.domain.model.common.TherapyAssetId
import com.simbiri.domain.model.common.Timestamp

/**
 * Metadata for a file used by a therapy session or module.
 *
 * storageKey identifies the durable object in object storage.
 */
data class TherapyAsset(
    val id: TherapyAssetId? = null,

    val role: TherapyAssetRole,
    val mediaType: TherapyMediaType,

    /**
     * Stable object-storage key, for example:
     *
     * therapy-content/sessions/{sessionId}/audio/intro.mp3
     */
    val storageKey: String,

    val mimeType: String,
    val sizeBytes: Long,

    /**
     * SHA-256 digest used to verify downloaded and cached content.
     */
    val sha256: String,

    /**
     * Optional BCP-47 locale such as "en", "en-KE", or "sw-KE".
     */
    val locale: String? = null,

    /**
     * Accessibility description for image assets.
     */
    val altText: String? = null,

    /**
     * Accessibility transcript for audio or video assets.
     */
    val transcript: String? = null,

    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

