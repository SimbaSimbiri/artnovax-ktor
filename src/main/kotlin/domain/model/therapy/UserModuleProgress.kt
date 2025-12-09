package com.simbiri.domain.model.therapy

import com.simbiri.domain.model.common.ModuleId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

data class UserModuleProgress(
    val moduleId: ModuleId,
    val userId: UserId,
    val progress: Double,
    val rating: Double?,
    val mediaCacheKey: String,
    val startedAt: Timestamp,
    val completedAt: Timestamp?,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)
