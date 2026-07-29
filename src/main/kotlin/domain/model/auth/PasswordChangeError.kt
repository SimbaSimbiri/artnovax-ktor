package com.simbiri.domain.model.auth

import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.Error

/**
 * Failures produced while changing an authenticated user's password.
 */
sealed interface PasswordChangeError : Error {

    /**
     * The supplied current password could not be verified.
     */
    data object InvalidCurrentPassword : PasswordChangeError

    /**
     * Reauthentication is temporarily blocked after repeated failures.
     */
    data object TemporarilyLocked : PasswordChangeError

    /**
     * The new password is identical to the current password.
     */
    data object NewPasswordMatchesCurrent : PasswordChangeError

    /**
     * The proposed new password violates password policy.
     */
    data class ValidationFailure(
        val error: DataError.ValidationError,
    ) : PasswordChangeError

    /**
     * Persistence or cryptographic infrastructure failed.
     */
    data class DataFailure(
        val error: DataError,
    ) : PasswordChangeError
}