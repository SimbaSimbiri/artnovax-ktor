package com.simbiri.domain.model.auth

import com.simbiri.domain.model.common.RefreshSessionId
import com.simbiri.domain.model.common.RefreshTokenFamilyId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

/**
 * Server-side state for one renewable authentication session.
 *
 * tokenHash contains only the SHA-256 digest of the opaque refresh token.
 * The plaintext refresh token must never be persisted.
 */
data class RefreshSession(
    val id: RefreshSessionId? = null,

    val familyId: RefreshTokenFamilyId,
    val userId: UserId,

    val tokenHash: String,

    /**
     * credential.sessionVersion at the time this refresh session was
     * issued.
     *
     * Password changes and logout-all-devices increment the credential
     * version, making this session stale.
     */
    val sessionVersion: Long,

    val expiresAt: Timestamp,
    val revokedAt: Timestamp? = null,

    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {

    init {
        require(
            TOKEN_HASH_PATTERN.matches(
                tokenHash
            )
        ) {
            "Refresh-session tokenHash must be a lowercase SHA-256 digest."
        }

        require(sessionVersion > 0L) {
            "Refresh-session sessionVersion must be positive."
        }

        if (createdAt != null) {
            require(
                expiresAt.isAfter(
                    createdAt
                )
            ) {
                "Refresh-session expiresAt must be after createdAt."
            }
        }

        if (createdAt != null && revokedAt != null) {
            require(
                !revokedAt.isBefore(
                    createdAt
                )
            ) {
                "Refresh-session revokedAt must not be before createdAt."
            }
        }
    }

    /**
     * Returns true when this session has not expired or been revoked.
     */
    fun isActiveAt(
        timestamp: Timestamp,
    ): Boolean = revokedAt == null && expiresAt.isAfter(timestamp)

    /**
     * Prevents the refresh-token digest from appearing in logs.
     */
    override fun toString(): String =
        "RefreshSession(id=$id, familyId=$familyId, userId=$userId, tokenHash=<redacted>, " +
                "sessionVersion=$sessionVersion, expiresAt=$expiresAt, revokedAt=$revokedAt, " +
                "createdAt=$createdAt, updatedAt=$updatedAt)"

    private companion object {
        val TOKEN_HASH_PATTERN = Regex("^[0-9a-f]{64}$")
    }
}
