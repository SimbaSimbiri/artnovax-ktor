package com.simbiri.domain.model.content

import com.simbiri.domain.model.common.*

data class Post(
    val id: PostId? = null,
    val communityId: CommunityId?,
    val creatorId: UserId,
    val artworkId: ArtworkId?, // if the post is sharing an artwork
    val parentPostId: PostId?, // if we want to maintain a thread, very important for user engagement
    val title: String?,
    val caption: String?,
    val upVoteCount: Int,
    val mediaUrls: List<String>,
    val isDeleted: Boolean,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)
