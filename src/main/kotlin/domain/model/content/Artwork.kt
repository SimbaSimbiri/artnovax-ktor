package com.simbiri.domain.model.content

import com.simbiri.domain.model.common.*
import com.simbiri.domain.model.therapy.MediaType

data class Artwork(
    val id: ArtworkId? = null,
    val userId: UserId,
    val sessionId: SessionId?,
    val moduleId: ModuleId?,
    val title: String?,
    val description: String?,
    val mediaType: MediaType,
    val assetUrl: String, // this will be from AWS S3
    val thumbnailUrl: String?,
    val isShared: Boolean,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)
