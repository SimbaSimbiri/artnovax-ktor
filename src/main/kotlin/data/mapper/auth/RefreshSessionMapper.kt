package com.simbiri.data.mapper.auth

import com.simbiri.data.database.entity.auth.RefreshSessionEntity
import com.simbiri.domain.model.auth.RefreshSession
import com.simbiri.domain.model.common.RefreshSessionId
import com.simbiri.domain.model.common.RefreshTokenFamilyId
import com.simbiri.domain.model.common.UserId
import java.time.Instant
import java.util.UUID

/**
 * Reconstructs a persisted refresh session.
 */
fun RefreshSessionEntity.toDomain(): RefreshSession = RefreshSession(
    id = RefreshSessionId(id),
    familyId = RefreshTokenFamilyId(
        familyId
    ),
    userId = UserId(userId),
    tokenHash = tokenHash,
    sessionVersion = sessionVersion,
    expiresAt = expiresAt,
    revokedAt = revokedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

/**
 * Prepares a new refresh session for insertion.
 */
fun RefreshSession.toEntityForCreate(
    now: Instant,
): RefreshSessionEntity = RefreshSessionEntity(
    id = UUID.randomUUID(),
    familyId = familyId.value,
    userId = userId.value,
    tokenHash = tokenHash,
    sessionVersion = sessionVersion,
    expiresAt = expiresAt,
    revokedAt = revokedAt,
    createdAt = now,
    updatedAt = now,
)
