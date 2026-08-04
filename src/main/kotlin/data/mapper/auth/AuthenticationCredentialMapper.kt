package com.simbiri.data.mapper.auth

import com.simbiri.data.database.entity.auth.AuthenticationCredentialEntity
import com.simbiri.domain.model.auth.AuthenticationCredential
import com.simbiri.domain.model.auth.PasswordHashAlgorithm
import com.simbiri.domain.model.common.UserId
import java.time.Instant

/**
 * Reconstructs a persisted authentication credential.
 */
fun AuthenticationCredentialEntity.toDomain(): AuthenticationCredential = AuthenticationCredential(
    userId = UserId(userId),
    passwordHash = passwordHash,
    passwordAlgorithm = PasswordHashAlgorithm.fromStorageValue(
            passwordAlgorithm
        ),
    passwordUpdatedAt = passwordUpdatedAt,
    failedLoginAttempts = failedLoginAttempts,
    lockedUntil = lockedUntil,
    sessionVersion = sessionVersion,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

/**
 * Prepares a new authentication credential for insertion.
 *
 */
fun AuthenticationCredential.toEntityForCreate(
    now: Instant,
): AuthenticationCredentialEntity = AuthenticationCredentialEntity(
    userId = userId.value,
    passwordHash = passwordHash,
    passwordAlgorithm = passwordAlgorithm.name,
    passwordUpdatedAt = passwordUpdatedAt,
    failedLoginAttempts = failedLoginAttempts,
    lockedUntil = lockedUntil,
    sessionVersion = sessionVersion,
    createdAt = now,
    updatedAt = now,
)
