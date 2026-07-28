package com.simbiri.domain.model.auth

import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.Error

/**
 * Failures specific to user authentication.
 *
 * InvalidCredentials deliberately does not distinguish between an unknown
 * email address, a missing credential, an inactive user, or an incorrect
 * password.
 */
sealed interface AuthenticationError : Error {

    data object InvalidCredentials : AuthenticationError

    data object TemporarilyLocked : AuthenticationError

    /**
     * Preserves unexpected persistence or infrastructure failures without
     * incorrectly presenting them as invalid credentials.
     */
    data class DataFailure( val error: DataError,) : AuthenticationError
}
