package com.simbiri.data.mapper.auth

import com.simbiri.data.database.entity.auth.AuthenticationCredentialEntity
import com.simbiri.data.database.entity.auth.AuthenticationCredentialTable
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
    createdAt = this[AuthenticationCredentialTable.createdAt],
    updatedAt = this[AuthenticationCredentialTable.updatedAt],
)
