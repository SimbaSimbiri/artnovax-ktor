package com.simbiri.data.mapper.auth

import com.simbiri.data.database.entity.auth.AuthenticationCredentialEntity
import com.simbiri.data.database.entity.auth.AuthenticationCredentialTable
import com.simbiri.data.database.entity.auth.RefreshSessionEntity
import com.simbiri.data.database.entity.auth.RefreshSessionTable
import org.jetbrains.exposed.sql.ResultRow

/**
 * Maps one Exposed result row to its persistence entity.
 */
fun ResultRow.toAuthenticationCredentialEntity(): AuthenticationCredentialEntity = AuthenticationCredentialEntity(
    userId = this[AuthenticationCredentialTable.userId],
    passwordHash = this[AuthenticationCredentialTable.passwordHash],
    passwordAlgorithm = this[AuthenticationCredentialTable.passwordAlgorithm],
    passwordUpdatedAt = this[AuthenticationCredentialTable.passwordUpdatedAt],
    failedLoginAttempts = this[AuthenticationCredentialTable.failedLoginAttempts],
    lockedUntil = this[AuthenticationCredentialTable.lockedUntil],
    sessionVersion = this[AuthenticationCredentialTable.sessionVersion],
    createdAt = this[AuthenticationCredentialTable.createdAt],
    updatedAt = this[AuthenticationCredentialTable.updatedAt],
)


/**
 * Maps one Exposed result row to its refresh-session entity.
 */
fun ResultRow.toRefreshSessionEntity(): RefreshSessionEntity = RefreshSessionEntity(
    id = this[RefreshSessionTable.id].value,
    familyId = this[RefreshSessionTable.familyId],
    userId = this[RefreshSessionTable.userId],
    tokenHash = this[RefreshSessionTable.tokenHash],
    sessionVersion = this[RefreshSessionTable.sessionVersion],
    expiresAt = this[RefreshSessionTable.expiresAt],
    revokedAt = this[RefreshSessionTable.revokedAt],
    createdAt = this[RefreshSessionTable.createdAt],
    updatedAt = this[RefreshSessionTable.updatedAt],
)