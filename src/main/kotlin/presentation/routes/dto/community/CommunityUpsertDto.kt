package com.simbiri.presentation.routes.dto.community

import com.simbiri.presentation.routes.dto.social.SocialLinkUpsertDto
import kotlinx.serialization.Serializable

@Serializable
data class CommunityUpsertDto(
    val ownerId: String,
    val name: String,
    val description: String,
    val profileUrl: String? = null,
    val tagline: String,
    val privateEvents: Boolean = true,
    val privatePosts: Boolean = true,
    val category: String? = null,
    val joinPermission: Int,        // 0 = AUTO, 1 = APPROVAL
    val approved: Boolean = false,
    val socialLinks: List<SocialLinkUpsertDto> = emptyList(),
)
