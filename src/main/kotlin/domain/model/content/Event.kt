package com.simbiri.domain.model.content

import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.common.EventId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

data class Event(
    val id: EventId,
    val communityId: CommunityId, // event must be from a community, by default everyone will belong to the ArtNovax community
    val creatorId: UserId,
    val title: String,
    val description: String,
    val isPrivate: Boolean,
    val startsAt: Timestamp,
    val endsAt: Timestamp,
    val posterImageUrl: String?,
    val location: String?, // if physical event
    val joinLink: String?, // if online event
    val capacity: Int?, // anticipated capacity
    val participantCount: Int,
    val upvotesCount: Int,
    val mediaUrls: List<String>?, // relevant media e.g barcode to scan, previous event pictures
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)
