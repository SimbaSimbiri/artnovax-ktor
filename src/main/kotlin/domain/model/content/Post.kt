package com.simbiri.domain.model.content

import com.simbiri.domain.model.common.ArtworkId
import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.common.PostId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

data class Post(
    val id: PostId,
    val communityId: CommunityId?,
    val creatorId: UserId,
    val artworkId: ArtworkId?, // if the post is sharing an artwork
    val parentPostId: PostId?, // if we want to maintain a thread
    val title: String?,
    val caption: String?,
    val upVoteCount: Int,
    val mediaUrls: List<String>,
    val isDeleted: Boolean,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)
