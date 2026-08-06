package com.simbiri.presentation.routes.dto.therapy.management

import kotlinx.serialization.Serializable

@Serializable
data class TherapyAssetUploadGrantResponseDto(
    val uploadUrl: String,
    val storageKey: String,
    val expiresAt: String,
    val requiredHeaders: Map<String, String>,
)
