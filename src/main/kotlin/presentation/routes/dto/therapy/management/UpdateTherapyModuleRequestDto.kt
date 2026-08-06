package com.simbiri.presentation.routes.dto.therapy.management

import kotlinx.serialization.Serializable

/**
 * Editable metadata for an existing therapy module.
 *
 * Ordering is managed by the dedicated reorder endpoint. Existing assets are preserved until the asset workflow is
 * implemented.
 */
@Serializable
data class UpdateTherapyModuleRequestDto(
    val title: String,
    val goal: String,
    val instructions: String,
    val whyThisHelps: String,
    val modality: String,
    val estimatedDurationSeconds: Int,
    val isSkippable: Boolean = false,
    val isRepeatable: Boolean = true,
)
