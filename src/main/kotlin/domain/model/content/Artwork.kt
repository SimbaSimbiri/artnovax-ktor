package com.simbiri.domain.model.content

import com.simbiri.domain.model.common.ArtworkId
import com.simbiri.domain.model.common.ModuleId
import com.simbiri.domain.model.common.SessionId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.therapy.MediaType

data class Artwork(
    val id: ArtworkId,
    val userId: UserId,
    val sessionId: SessionId?,
    val moduleId: ModuleId?,
    val title: String?,
    val description: String?,
    val mediaType: MediaType,
    val mediaUrl: String, // this will be from AWS S3
    val thumbnailUrl: String?,
    val isShared: Boolean,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)
