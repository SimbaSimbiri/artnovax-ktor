package com.simbiri.presentation.routes.dto.therapy.public

import kotlinx.serialization.Serializable

/**
 * One executable module within a published therapy session.
 */
@Serializable
data class PublishedTherapyModuleResponseDto(
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
    val assets: List<PublishedTherapyAssetResponseDto>,
)
