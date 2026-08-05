package com.simbiri.domain.repository

import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Deletes refresh-session records that are no longer required for token
 * validation or replay detection.
 */
interface RefreshSessionCleanupRepository {

    /**
     * Deletes at most [limit] sessions whose token expiry is at or before
     * [expiredBefore].
     *
     * This bounded operations will prevent one cleanup run from holding a large
     * database transaction.
     */
    suspend fun deleteSessionsExpiredBefore(
        expiredBefore: Timestamp,
        limit: Int,
    ): ResultType<Int, DataError>
}
