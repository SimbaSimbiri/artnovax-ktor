package com.simbiri.domain.model.messaging

import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.common.ConversationId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

data class Conversation(
    val id: ConversationId,
    val type: ConversationType, // can be direct, group or community
    val communityId: CommunityId?, // if conversation belongs to a community/group
    val createdBy: UserId,
    val title: String?, // name of the Conversation e.g. UoN ArtNovax
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)