package com.simbiri.presentation.routes.dto.community

import kotlinx.serialization.Serializable


@Serializable
data class CommunityMemberUpsertDto(
    val userId: String,
    val participantRole: String, // "OWNER", "MODERATOR", "MEMBER"
)
