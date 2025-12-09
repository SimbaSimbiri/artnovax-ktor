package com.simbiri.domain.model.messaging

import com.simbiri.domain.model.common.ConversationId
import com.simbiri.domain.model.common.MessageId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

data class Message(
    val id: MessageId,
    val conversationId: ConversationId,
    val senderId: UserId,
    val replyToMessageId: MessageId?,
    val content: String,
    val isSystemMessage: Boolean,
    val isDeleted: Boolean,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)
