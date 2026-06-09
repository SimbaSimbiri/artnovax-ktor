package com.simbiri.domain.model.messaging

import com.simbiri.domain.model.common.ConversationId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.community.CommunityParticipantRole

data class ConversationParticipant(
    val conversationId: ConversationId,
    val userId: UserId,
    val role: ConversationParticipantRole,
    val joinedAt: Timestamp,
    val leftAt: Timestamp?
)
