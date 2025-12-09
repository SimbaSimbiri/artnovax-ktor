package com.simbiri.domain.model.messaging

import com.simbiri.domain.model.common.MessageId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

data class MessageReaction(
    val messageId: MessageId,
    val userId: UserId,
    val reactionEmoji: String,
    val createdAt: Timestamp
)
