package com.simbiri.domain.model.therapy

import com.simbiri.domain.model.common.SessionId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

data class UserSession(
    val sessionId: SessionId,
    val userId: UserId,
    val progress: Double,
    val rating: Double?,
    val phqRatingBegin: Int?,
    val phqRatingEnd: Int?,
    val startedAt: Timestamp,
    val completedAt: Timestamp,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)
