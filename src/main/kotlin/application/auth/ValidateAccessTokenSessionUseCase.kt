package com.simbiri.application.auth

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.repository.AccessTokenSessionRepository
import com.simbiri.domain.util.ResultType

/**
 * Confirms that a cryptographically valid JWT still represents a current
 * server-side authentication session.
 *
 * Authentication fails closed when persisted state cannot be confirmed.
 */
class ValidateAccessTokenSessionUseCase(
    private val accessTokenSessionRepository: AccessTokenSessionRepository,
) {

    suspend operator fun invoke(
        userId: UserId,
        sessionVersion: Long,
    ): Boolean {
        if (sessionVersion <= 0L) {
            return false
        }

        return when (val result = accessTokenSessionRepository.isCurrent(
                userId = userId,
                sessionVersion = sessionVersion,
            )) {
            is ResultType.Success -> result.data

            /*
             * Database and infrastructure failures reject the token rather
             * than allowing authentication to continue.
             */
            is ResultType.Failure -> false
        }
    }
}
