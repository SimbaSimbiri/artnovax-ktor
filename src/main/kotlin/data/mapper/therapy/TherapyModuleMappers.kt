package com.simbiri.data.mapper.therapy

import com.simbiri.data.database.entity.therapy.TherapyModuleEntity
import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.therapy.TherapyAsset
import com.simbiri.domain.model.therapy.TherapyModality
import com.simbiri.domain.model.therapy.TherapyModule
import java.time.Instant
import java.util.*


fun TherapyModuleEntity.toDomain(
    assets: List<TherapyAsset> = emptyList(),
): TherapyModule = TherapyModule(
    id = TherapyModuleId(id),
    orderIndex = orderIndex,
    title = title,
    goal = goal,
    instructions = instructions,
    whyThisHelps = whyThisHelps,
    modality = TherapyModality.valueOf(modalityName),
    estimatedDurationSeconds = estimatedDurationSeconds,
    isSkippable = isSkippable,
    isRepeatable = isRepeatable,
    assets = assets,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun TherapyModule.toEntity(
    therapySessionId: UUID,
    now: Instant,
): TherapyModuleEntity = TherapyModuleEntity(
    id = id?.value ?: UUID.randomUUID(),
    therapySessionId = therapySessionId,
    orderIndex = orderIndex,
    title = title.trim(),
    goal = goal.trim(),
    instructions = instructions.trim(),
    whyThisHelps = whyThisHelps.trim(),
    modalityName = modality.name,
    estimatedDurationSeconds = estimatedDurationSeconds,
    isSkippable = isSkippable,
    isRepeatable = isRepeatable,
    createdAt = createdAt ?: now,
    updatedAt = now,
)
