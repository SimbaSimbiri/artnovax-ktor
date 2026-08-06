package com.simbiri.domain.storage

data class TherapyAssetUploadSpecification(
    val storageKey: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
)
