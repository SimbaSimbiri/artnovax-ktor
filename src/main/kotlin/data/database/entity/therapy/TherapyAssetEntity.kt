package com.simbiri.data.database.entity.therapy

import java.time.Instant
import java.util.UUID

data class TherapyAssetEntity(
    val id: UUID,
    val therapySessionId: UUID,
    val therapyModuleId: UUID?,

    val roleName: String,
    val mediaTypeName: String,

    val storageKey: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,

    val locale: String?,
    val altText: String?,
    val transcript: String?,

    val createdAt: Instant,
    val updatedAt: Instant,
)
