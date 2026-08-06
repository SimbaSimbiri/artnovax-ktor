package com.simbiri.domain.storage

import java.time.Instant

data class TherapyAssetUploadGrant(
    val uploadUrl: String,
    val storageKey: String,
    val expiresAt: Instant,
    val requiredHeaders: Map<String, String>,
)
