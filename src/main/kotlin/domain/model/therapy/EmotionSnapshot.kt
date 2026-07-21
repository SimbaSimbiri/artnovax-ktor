package com.simbiri.domain.model.therapy

import com.simbiri.domain.model.common.EmotionSnapshotId
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

data class EmotionSnapshot(
    val id: EmotionSnapshotId,
    val userId: UserId,
    val therapySessionId: TherapySessionId,
    val timestamp: Timestamp,
    val primaryEmotion: EmotionType,
    val phase: EmotionSnapshotPhase,
    val confidence: Double,
    val rawPayload: String?,
    val createdAt: Timestamp
)
