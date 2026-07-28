package com.simbiri.presentation.routes.dto.therapy.public

import kotlinx.serialization.Serializable


/**
 * Complete published therapy-session response.
 */
@Serializable
data class PublishedTherapySessionResponseDto(
    val id: String,
    val seriesId: String,

    val title: String,
    val description: String,
    val tagline: String?,

    val intensity: String,
    val locale: String,
    val version: Int,
    val therapeuticPriority: String,

    val goalTags: List<String>,
    val contraindications: List<String>,
    val cultureTags: List<String>,

    val coverAsset: PublishedTherapyAssetResponseDto?,
    val modules: List<PublishedTherapyModuleResponseDto>,

    val moduleCount: Int,
    val estimatedDurationSeconds: Int,
    val estimatedDurationMinutes: Int,

    val publishedAt: String,
)