package com.simbiri.domain.model.auth

import com.simbiri.domain.model.common.RefreshSessionId
import com.simbiri.domain.model.common.UserId

/**
 * Result of atomically consuming one refresh token.
 */
sealed interface RefreshSessionRotationResult {

    data class Rotated(
        val refreshSessionId: RefreshSessionId,
        val userId: UserId,
        val sessionVersion: Long,
    ) : RefreshSessionRotationResult

    /**
     * The token is unknown, expired, belongs to an inactive user, or has a
     * stale credential session version.
     */
    data object Invalid : RefreshSessionRotationResult

    /**
     * A previously consumed token was presented again.
     *
     * The repository revokes the entire token family before returning this
     * result.
     */
    data object ReuseDetected : RefreshSessionRotationResult
}
