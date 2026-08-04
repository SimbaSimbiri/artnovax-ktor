package com.simbiri.data.database.entity.auth

import java.time.Instant
import java.util.UUID

/**
 * Persistence representation of one refresh session.
 */
data class RefreshSessionEntity(
    val id: UUID,

    val familyId: UUID,
    val userId: UUID,

    val tokenHash: String,
    val sessionVersion: Long,

    val expiresAt: Instant,
    val revokedAt: Instant?,

    val createdAt: Instant,
    val updatedAt: Instant,
)
