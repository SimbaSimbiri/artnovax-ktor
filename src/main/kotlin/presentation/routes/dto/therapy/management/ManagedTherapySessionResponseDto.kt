package com.simbiri.presentation.routes.dto.therapy.management

import kotlinx.serialization.Serializable


/**
 * Complete aggregate representation used by management detail
 * endpoints.
 */
@Serializable
data class ManagedTherapySessionResponseDto(
    val id: String,
    val seriesId: String,
    val authorId: String,

    val title: String,
    val description: String,
    val tagline: String?,

    val status: String,
    val version: Int,

    val therapeuticPriority: String,
    val intensity: String,
    val locale: String,

    val goalTags: List<String>,
    val contraindications: List<String>,
    val cultureTags: List<String>,

    val coverAsset: ManagedTherapyAssetResponseDto?,
    val modules: List<ManagedTherapyModuleResponseDto>,

    val moduleCount: Int,
    val estimatedDurationSeconds: Int,
    val estimatedDurationMinutes: Int,

    val createdAt: String,
    val updatedAt: String,
    val publishedAt: String?,
    val archivedAt: String?,
)
