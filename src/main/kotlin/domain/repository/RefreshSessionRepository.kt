package com.simbiri.domain.repository

import com.simbiri.domain.model.auth.RefreshSession
import com.simbiri.domain.model.common.RefreshSessionId
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
}
