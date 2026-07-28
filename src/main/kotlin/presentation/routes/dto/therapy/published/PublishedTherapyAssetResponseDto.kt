package com.simbiri.presentation.routes.dto.therapy.public

import kotlinx.serialization.Serializable

/**
 * Public metadata for an asset attached to published therapy content.
 *
 * storageKey is intentionally omitted. Asset delivery will occur through
 * a dedicated asset-access endpoint or signed-URL service.
 */
@Serializable
data class PublishedTherapyAssetResponseDto(
    val id: String,
    val role: String,
    val mediaType: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
    val locale: String?,
    val altText: String?,
    val transcript: String?,
)
