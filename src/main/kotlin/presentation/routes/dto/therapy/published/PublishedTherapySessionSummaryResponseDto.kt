package com.simbiri.presentation.routes.dto.therapy.published

import kotlinx.serialization.Serializable

/**
 * Compact representation used by public therapy catalogue lists.
 *
 * Module content is excluded so catalogue requests do not return the
 * complete guided-session payload for every result.
 */
@Serializable
data class PublishedTherapySessionSummaryResponseDto(
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

    val moduleCount: Int,
    val estimatedDurationSeconds: Int,
    val estimatedDurationMinutes: Int,

    val publishedAt: String,
)
