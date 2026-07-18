package com.simbiri.domain.model.therapy

import com.simbiri.domain.model.common.ModuleId
import com.simbiri.domain.model.common.SessionId
import com.simbiri.domain.model.common.Timestamp

data class TherapyModule(
    val id: ModuleId? = null,
    val sessionId: SessionId,
    val orderIndex: Int, // if it's within a TherapySession recommended for higher pHQ, prioritize AFFIRMATION and BREATHING modules
    val title: String,
    val instructions: String,
    val moduleType: ModuleType,
    val primaryMediaType: MediaType, // if module type is Breathing, MediaType will be VIDEO, if doodling, CANVAS
    val primaryMediaUrl: String?,
    val backgroundAudioUrl: String?, // to enhance sole therapeutic effect, we can put background narrator for affirmations
    val backgroundVideoUrl: String?,
    val backgroundImageUrl: String?,
    val avgRating: Double?, // this will be aggregated from TherapyModuleProgress.Rating
    val avgCompletionTimeMins: Double?, // this will be aggregate from TherapyModuleProgress.startedAt - TherapyModuleProgress.completedAt
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)
