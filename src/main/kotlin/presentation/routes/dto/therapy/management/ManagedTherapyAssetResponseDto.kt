package com.simbiri.presentation.routes.dto.therapy.management

import kotlinx.serialization.Serializable

/**
 * Complete internal asset metadata used by therapy-content authors,
 * reviewers, and publishers.
 */
@Serializable
data class ManagedTherapyAssetResponseDto(
    val id: String,
    val role: String,
    val mediaType: String,

    val storageKey: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,

    val locale: String?,
    val altText: String?,
    val transcript: String?,

    val createdAt: String,
    val updatedAt: String,
)
