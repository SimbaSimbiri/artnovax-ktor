package com.simbiri.domain.repository

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Mutates server controlled access token session state
 */
interface AccessTokenSessionCommandRepository {

    /**
     * Invalidated every access token issued for the user.
     *
     * The persisted session version must be incremented atomically so concurrent invalidation requestions cannot
     * overwrite one another
     */
    suspend fun invalidateAllSessions(
        userId: UserId,
    ): ResultType<Unit, DataError>
}