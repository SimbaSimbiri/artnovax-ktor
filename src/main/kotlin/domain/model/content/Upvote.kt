package com.simbiri.domain.model.content

import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

data class Upvote(
    val id: Long, // we don't need id to be universally unique hence we can just use a long, will be faster for indexing
    val userId: UserId,
    val targetType: UpvoteTargetType,
    val targetId: String?, // we store the event or postId; mapper will handle relevant conversion based on target
    val createdAt: Timestamp,
)
