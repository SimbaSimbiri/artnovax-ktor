package com.simbiri.presentation.routes.dto.community

import kotlinx.serialization.Serializable

@Serializable
data class CommunityMemberResponseDto(
    val userId: String,
    val communityId: String,
    val joinedAt: String,
    val leftAt: String?,
    val userTypeAtJoin: Int?,
    val participantRole: String,
)
