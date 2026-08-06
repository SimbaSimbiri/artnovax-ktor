package com.simbiri.presentation.routes.dto.therapy

import com.simbiri.domain.storage.TherapyAssetDownloadGrant
import kotlinx.serialization.Serializable

@Serializable
data class TherapyAssetDownloadGrantResponseDto(
    val downloadUrl: String,
    val expiresAt: String,
)

fun TherapyAssetDownloadGrant.toResponseDto(): TherapyAssetDownloadGrantResponseDto =
    TherapyAssetDownloadGrantResponseDto(
        downloadUrl = downloadUrl,
        expiresAt = expiresAt.toString(),
    )
