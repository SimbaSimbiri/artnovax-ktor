package com.simbiri.domain.model.auth

import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.Error

/**
 * Failures produced while rotating a refresh token.
 */
sealed interface RefreshAuthenticationError : Error {

    /**
     * Includes unknown, malformed, expired, stale, revoked, and reused
     * refresh tokens.
     */
    data object InvalidRefreshToken : RefreshAuthenticationError

    data class DataFailure(
        val error: DataError,
    ) : RefreshAuthenticationError
}
