package com.simbiri.domain.model.community

import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.social.SocialLink

data class Community(
    val id: CommunityId,
    val ownerId: UserId,
    val moderatorId: UserId?,
    val name: String,
    val description: String,
    val profileUrl: String?,
    val memberCount: Int,
    val joinPermission: JoinPermission,
    val chatBackgroundUrl: String?,
    val tagline: String,
    val privateEvents: Boolean,
    val privatePosts: Boolean,
    val category: String?,
    val approved: Boolean,
    val socialLinks: List<SocialLink>,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)