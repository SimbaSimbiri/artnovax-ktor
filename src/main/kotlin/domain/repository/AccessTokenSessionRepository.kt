package com.simbiri.domain.repository

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Reads the minimum persisted state required to validate an access-token
 * session.
 */
interface AccessTokenSessionRepository {

    /**
     * Returns true only when:
     * - the user exists;
     * - the user is active;
     * - the credential exists;
     * - the persisted session version matches the JWT claim.
     */
    suspend fun isCurrent(
        userId: UserId,
        sessionVersion: Long,
    ): ResultType<Boolean, DataError>
}
