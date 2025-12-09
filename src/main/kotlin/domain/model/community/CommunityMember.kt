package com.simbiri.domain.model.community

import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.user.UserType

data class CommunityMember(
    val userId: UserId,
    val communityId: CommunityId,
    val joinedAt: Timestamp,
    val leftAt: Timestamp?,
    val userTypeAtJoin: UserType? = null, // Needed for moderation ...admin/psychologist can delete inappropriate texts
)