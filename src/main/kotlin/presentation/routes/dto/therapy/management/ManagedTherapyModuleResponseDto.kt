package com.simbiri.presentation.routes.dto.therapy.management

import kotlinx.serialization.Serializable

/**
 * Complete module representation used by the management API.
 */
@Serializable
data class ManagedTherapyModuleResponseDto(
    val id: String,
    val orderIndex: Int,

    val title: String,
    val goal: String,
    val instructions: String,
    val whyThisHelps: String,

    val modality: String,
    val estimatedDurationSeconds: Int,

    val isSkippable: Boolean,
    val isRepeatable: Boolean,

    val assets: List<ManagedTherapyAssetResponseDto>,

    val createdAt: String,
    val updatedAt: String,
)

