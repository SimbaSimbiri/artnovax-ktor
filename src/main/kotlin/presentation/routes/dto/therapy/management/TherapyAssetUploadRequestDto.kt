package com.simbiri.presentation.routes.dto.therapy.management

import kotlinx.serialization.Serializable

@Serializable
data class TherapyAssetUploadRequestDto(
    val therapyModuleId: String? = null,
    val role: String,
    val mediaType: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
    val locale: String? = null,
    val altText: String? = null,
    val transcript: String? = null,
)
