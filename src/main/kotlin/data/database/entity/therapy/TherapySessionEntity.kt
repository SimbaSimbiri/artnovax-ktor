package com.simbiri.data.database.entity.therapy

import java.time.Instant
import java.util.UUID

data class TherapySessionEntity(
    val id: UUID,
    val seriesId: UUID,
    val authorId: UUID,

    val title: String,
    val description: String,
    val tagline: String?,

    val statusName: String,
    val version: Int,

    val therapeuticPriorityName: String,
    val intensityName: String,
    val locale: String,

    val createdAt: Instant,
    val updatedAt: Instant,
    val publishedAt: Instant?,
    val archivedAt: Instant?,
)
