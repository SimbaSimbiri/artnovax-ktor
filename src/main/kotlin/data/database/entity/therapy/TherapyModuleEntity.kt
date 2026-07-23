package com.simbiri.data.database.entity.therapy

import java.time.Instant
import java.util.UUID

data class TherapyModuleEntity(
    val id: UUID,
    val therapySessionId: UUID,

    val orderIndex: Int,
    val title: String,
    val goal: String,
    val instructions: String,
    val whyThisHelps: String,

    val modalityName: String,
    val estimatedDurationSeconds: Int,

    val isSkippable: Boolean,
    val isRepeatable: Boolean,

    val createdAt: Instant,
    val updatedAt: Instant,
)
