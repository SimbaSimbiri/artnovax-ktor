package com.simbiri.domain.model.messaging

import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.common.ConversationId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

data class Conversation(
    val id: ConversationId,
    val type: ConversationType,
    val communityId: CommunityId?,
    val createdBy: UserId,
    val title: String?,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)