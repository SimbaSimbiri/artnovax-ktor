package com.simbiri.domain.repository

import com.simbiri.domain.model.auth.RefreshSession
import com.simbiri.domain.model.auth.RefreshSessionRotationResult
import com.simbiri.domain.model.common.RefreshSessionId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Persistence operations for renewable authentication sessions.
 */
interface RefreshSessionRepository {

    suspend fun createSession(
        session: RefreshSession,
    ): ResultType<
            RefreshSessionId,
            DataError,
            >

    /**
     * Returns both active and revoked sessions.
     *
     * The refresh workflow must be able to detect attempted reuse of a
     * previously rotated token.
     */
    suspend fun getSessionByTokenHash(
        tokenHash: String,
    ): ResultType<
            RefreshSession,
            DataError,
            >

    /**
     * Atomically consumes one refresh token and creates its replacement.
     *
     * The implementation must lock the current session row so concurrent
     * requests cannot both rotate the same token successfully.
     */
    suspend fun rotateSession(
        presentedTokenHash: String,
        replacementTokenHash: String,
        replacementExpiresAt: Timestamp,
    ): ResultType<
            RefreshSessionRotationResult,
            DataError,
            >

    /**
     * Revokes the complete rotation family containing the supplied token hash.
     *
     * Unknown and already-revoked tokens are treated as successful no-ops so
     * logout remains idempotent and does not disclose token validity.
     */
    suspend fun revokeFamilyByTokenHash(
        tokenHash: String,
    ): ResultType<Unit, DataError>
}
