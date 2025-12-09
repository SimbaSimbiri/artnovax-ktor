package com.simbiri.domain.model.content

import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

data class Upvote(
    val id: Long,
    val userId: UserId,
    val targetType: UpvoteTargetType,
    val targetId: String?, // we store the event or postId; mapper will handle relevant conversion based on target
    val createdAt: Timestamp,
)

enum class UpvoteTargetType{
    EVENT,
    POST
}
