package com.simbiri.data.database.entity.social

import java.time.Instant
import java.util.*

data class SocialLinkEntity(
    val id: UUID,
    val userId: UUID,
    val platformId: Int,
    val username: String,
    val completeUrl: String,
    val createdAt: Instant,
)
