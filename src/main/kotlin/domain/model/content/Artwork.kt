package com.simbiri.domain.model.content

import com.simbiri.domain.model.common.*
import com.simbiri.domain.model.therapy.TherapyMediaType

data class Artwork(
    val id: ArtworkId? = null,
    val userId: UserId,
    val therapySessionId: TherapySessionId?,
    val therapyModuleId: TherapyModuleId?,
    val title: String?,
    val description: String?,
    val therapyMediaType: TherapyMediaType,
    val assetUrl: String, // this will be from AWS S3
    val thumbnailUrl: String?,
    val isShared: Boolean,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)
