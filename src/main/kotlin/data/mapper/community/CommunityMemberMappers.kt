package com.simbiri.data.mapper.community

import com.simbiri.data.database.entity.community.CommunityMemberEntity
import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.community.CommunityMember
import com.simbiri.domain.model.community.CommunityParticipantRole
import com.simbiri.domain.model.user.UserType

fun CommunityMemberEntity.toDomain(): CommunityMember =
    CommunityMember(
        userId = UserId(userId),
        communityId = CommunityId(communityId),
        joinedAt = joinedAt,
        leftAt = leftAt,
        userTypeAtJoin = userTypeAtJoinCode?.let{ UserType.fromCode(it) },
        commParticipantRole = CommunityParticipantRole.valueOf(commParticipantRole),
    )