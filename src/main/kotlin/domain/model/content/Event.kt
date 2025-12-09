package com.simbiri.domain.model.content

import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.common.EventId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

data class Event(
    val id: EventId,
    val communityId: CommunityId,
    val creatorId: UserId,
    val title: String,
    val description: String,
    val isPrivate: Boolean,
    val startsAt: Timestamp,
    val endsAt: Timestamp,
    val posterImageUrl: String?,
    val location: String?,
    val joinLink: String?,
    val capacity: Int?,
    val participantCount: Int,
    val upvotesCount: Int,
    val mediaUrls: List<String>?, // relevant media e.g barcode to scan, previous event pictures
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)
