package com.simbiri.domain.model.therapy

import com.simbiri.domain.model.common.SessionId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

data class TherapySession(
    val id: SessionId,
    val ownerId: UserId,
    val title:String,
    val description:String,
    val tagline:String?,
    val moduleCount: Int,
    val avgRating: Double?,
    val avdCompletionTimeMinutes: Double?,
    val phqRating: Double?,
    val isPublished: Boolean,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)
