package com.simbiri.domain.model.therapy

import com.simbiri.domain.model.common.ModuleId
import com.simbiri.domain.model.common.SessionId
import com.simbiri.domain.model.common.Timestamp

data class Module(
    val id: ModuleId,
    val sessionId: SessionId,
    val orderIndex: Int,
    val title: String,
    val instructions: String,
    val moduleType: ModuleType,
    val primaryMediaType: MediaType,
    val primaryMediaUrl: String?,
    val backgroundAudioUrl: String?,
    val backgroundVideoUrl: String?,
    val backgroundImageUrl: String?,
    val avgRating: Double?,
    val avgCompletionTimeMins: Double?,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)
