package com.simbiri.data.database.entity.community

import java.time.Instant
import java.util.UUID

data class CommunityMemberEntity(
    val userId: UUID,
    val communityId: UUID,
    val joinedAt: Instant,
    val leftAt: Instant?,
    val userTypeAtJoinCode: Int?,
    val participantRole: String,
)

