package com.simbiri.domain.model.community

import com.simbiri.domain.model.common.UserId

/**
 * Represents an instruction to add a user to a community.
 *
 * Unlike CommunityMember, this doesn't contain persistence-managed
 * metadata such as joinedAt, leftAt, or userTypeAtJoin.
 */
data class CommunityMemberAssignment(
    val userId: UserId,
    val role: CommunityParticipantRole,
)