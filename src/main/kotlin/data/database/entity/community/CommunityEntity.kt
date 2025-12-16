package com.simbiri.data.database.entity.community

import java.util.UUID
import java.time.Instant


data class CommunityEntity(
    val id: UUID,
    val ownerId: UUID,
    val name: String,
    val description: String,
    val profileUrl: String?,
    val memberCount: Int,
    val joinPermissionCode: Int,
    val chatBackgroundUrl: String?,
    val tagline: String,
    val privateEvents: Boolean,
    val privatePosts: Boolean,
    val category: String?,
    val approved: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
