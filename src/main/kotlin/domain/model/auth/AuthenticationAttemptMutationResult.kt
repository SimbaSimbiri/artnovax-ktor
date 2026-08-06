package com.simbiri.domain.model.auth

/**
 * Result of atomically mutating login-attempt state.
 */
sealed interface AuthenticationAttemptMutationResult {

    /**
     * The mutation applied to the expected credential version.
     */
    data class Applied(
        val sessionVersion: Long,
    ) : AuthenticationAttemptMutationResult

    /**
     * When credential changed after the caller loaded it.
     * This can happen when the password or session version changes
     * concurrently.
     */
    data object StaleCredential : AuthenticationAttemptMutationResult

    /**
     * The credential became locked before the mutation obtained its row lock.
     */
    data object TemporarilyLocked : AuthenticationAttemptMutationResult
}
