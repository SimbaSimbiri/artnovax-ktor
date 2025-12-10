package com.simbiri.domain.model.therapy

import com.simbiri.domain.model.common.ModuleId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

data class UserModule(
    val moduleId: ModuleId,
    val userId: UserId,
    val progress: Double,
    val rating: Double?,
    val mediaCacheKey: String, // we will cache the media to Redis to persist progress to the cloud e.g. painting progress
    val startedAt: Timestamp,
    val completedAt: Timestamp?, // this can be null to allow user to come back and finish later
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)
