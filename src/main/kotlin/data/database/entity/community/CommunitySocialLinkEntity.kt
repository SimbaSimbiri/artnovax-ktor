package com.simbiri.data.database.entity.community

import java.time.Instant
import java.util.UUID

data class CommunitySocialLinkEntity(
    val id: UUID,
    val communityId: UUID,
    val platformId: Int,
    val username: String,
    val completeUrl: String,
    val createdAt: Instant,
)
