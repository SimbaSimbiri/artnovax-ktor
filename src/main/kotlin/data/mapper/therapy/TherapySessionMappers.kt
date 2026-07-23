package com.simbiri.data.mapper.therapy

import com.simbiri.data.database.entity.therapy.TherapySessionEntity
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.TherapySessionSeriesId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.therapy.*
import java.time.Instant
import java.util.*

fun TherapySessionEntity.toDomain(
    modules: List<TherapyModule> = emptyList(),
    goalTags: Set<TherapyGoal> = emptySet(),
    contraindications: Set<TherapyContraindication> = emptySet(),
    cultureTags: Set<String> = emptySet(),
    coverAsset: TherapyAsset? = null,
): TherapySession = TherapySession(
    id = TherapySessionId(id),
    seriesId = TherapySessionSeriesId(seriesId),
    authorId = UserId(authorId),
    title = title,
    description = description,
    tagline = tagline,
    status = TherapyContentStatus.valueOf(statusName),
    version = version,
    therapeuticPriority = TherapeuticPriority.valueOf(
        therapeuticPriorityName
    ),
    intensity = TherapyIntensity.valueOf(intensityName),
    locale = locale,
    goalTags = goalTags,
    contraindications = contraindications,
    cultureTags = cultureTags,
    coverAsset = coverAsset,
    modules = modules.sortedBy { module ->
        module.orderIndex
    },
    createdAt = createdAt,
    updatedAt = updatedAt,
    publishedAt = publishedAt,
    archivedAt = archivedAt,
)

fun TherapySession.toEntity(
    now: Instant,
): TherapySessionEntity = TherapySessionEntity(
    id = id?.value ?: UUID.randomUUID(),
    seriesId = seriesId?.value ?: UUID.randomUUID(),
    authorId = authorId.value,
    title = title.trim(),
    description = description.trim(),
    tagline = tagline?.trim(),
    statusName = status.name,
    version = version,
    therapeuticPriorityName = therapeuticPriority.name,
    intensityName = intensity.name,
    locale = locale.trim(),
    createdAt = createdAt ?: now,
    updatedAt = now,
    publishedAt = publishedAt,
    archivedAt = archivedAt,
)
