package com.simbiri.domain.model.therapy

import com.simbiri.domain.model.common.SessionId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

data class TherapySession(
    val id: SessionId? = null,
    val ownerId: UserId, // must be restricted to be of userType(3) or above
    val title:String, // e.g. Ubuntu Flow for African themed session with Poems, Bantu Audio e.t.c.
    val description:String,
    val tagline:String?,
    val moduleCount: Int,
    val avgRating: Double?, // this will be aggregated from individual TherapySessionRun.Ratings
    val avgCompletionTimeMinutes: Double?, // this will be aggregate from TherapySessionRun.startedAt - TherapySessionRun.completedAt
    val phqRating: Double?, // If user has a higher pHQ rating than 3, recommend a more calming session
    val isPublished: Boolean, // will allow for psychologists to draft their Sessions before publishing it
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)
