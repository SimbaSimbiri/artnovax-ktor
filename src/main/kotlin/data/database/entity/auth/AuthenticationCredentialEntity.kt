package com.simbiri.data.database.entity.auth

import java.time.Instant
import java.util.UUID

/**
 * Persistence representation of one user's authentication credential.
 */
data class AuthenticationCredentialEntity(
    val userId: UUID,

    val passwordHash: String,
    val passwordAlgorithm: String,
    val passwordUpdatedAt: Instant,

    val failedLoginAttempts: Int,
    val lockedUntil: Instant?,
    val sessionVersion: Long,

    val createdAt: Instant,
    val updatedAt: Instant,
)
