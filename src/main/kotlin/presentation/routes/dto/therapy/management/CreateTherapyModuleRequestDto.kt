package com.simbiri.presentation.routes.dto.therapy.management

import kotlinx.serialization.Serializable

/**
 * Editable values used to create one module in a therapy-session draft.
 *
 * Identity, assets, and persistence timestamps are controlled by the server.
 */
@Serializable
data class CreateTherapyModuleRequestDto(
    val orderIndex: Int,
    val title: String,
    val goal: String,
    val instructions: String,
    val whyThisHelps: String,
    val modality: String,
    val estimatedDurationSeconds: Int,
    val isSkippable: Boolean = false,
    val isRepeatable: Boolean = true,
)
