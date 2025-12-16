package com.simbiri.domain.model.community

import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.social.SocialLink

data class Community(
    val id: CommunityId? = null,
    val ownerId: UserId,
    val name: String,
    val description: String,
    val profileUrl: String?,
    val memberCount: Int,
    val joinPermission: JoinPermission,
    val chatBackgroundUrl: String?,
    val tagline: String,
    val privateEvents: Boolean, // only users in the community will see broadcasted events
    val privatePosts: Boolean,  // only users in the community will see posts on their fyp
    val category: String?,
    val approved: Boolean,
    val socialLinks: List<SocialLink>,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)