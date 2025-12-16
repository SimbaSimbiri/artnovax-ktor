package com.simbiri.presentation.routes.dto.community

import com.simbiri.presentation.routes.dto.social.SocialLinkResponseDto
import kotlinx.serialization.Serializable

@Serializable
data class CommunityResponseDto(
    val id: String,
    val ownerId: String,
    val name: String,
    val description: String,
    val profileImageUrl: String?,
    val chatBackgroundImageUrl: String?,
    val tagline: String,
    val memberCount: Int,
    val privateEvents: Boolean,
    val privatePosts: Boolean,
    val category: String?,
    val joinPermission: String,
    val approved: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val socialLinks: List<SocialLinkResponseDto>,
)
