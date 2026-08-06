package com.simbiri.presentation.routes.dto.therapy.management

import kotlinx.serialization.Serializable

/**
 * Editable metadata for a therapy-session draft.
 *
 * Identity, ownership, lifecycle state, versioning, modules, assets, and
 * persistence timestamps are controlled by the server.
 */
@Serializable
data class TherapyDraftMetadataRequestDto(
    val title: String,
    val description: String,
    val tagline: String? = null,

    val therapeuticPriority: String = "MENTAL_HEALTH",
    val intensity: String,
    val locale: String,

    val goalTags: List<String> = emptyList(),
    val contraindications: List<String> = emptyList(),
    val cultureTags: List<String> = emptyList(),
)
