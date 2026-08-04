package com.simbiri.application.auth

import com.simbiri.domain.repository.RefreshSessionRepository
import com.simbiri.domain.security.RefreshTokenIssuer
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Revokes the refresh-token family belonging to one client login session.
 *
 * Malformed, unknown, expired, and already-revoked tokens are successful
 * no-ops so callers cannot use logout to probe refresh-token validity.
 */
class LogoutCurrentDeviceUseCase(
    private val refreshSessionRepository: RefreshSessionRepository,

    private val refreshTokenIssuer: RefreshTokenIssuer,
) {

    suspend operator fun invoke(
        refreshToken: String,
    ): ResultType<Unit, DataError> {
        val tokenHash = try {
            refreshTokenIssuer.hash(
                refreshToken
            )
        } catch (_: Exception) {
            /*
            * Logout is deliberately idempotent. A malformed token gives
            * the client the same result as an already-ended session.
            */
            return ResultType.Success(
                Unit
            )
        }

        return refreshSessionRepository.revokeFamilyByTokenHash(
                tokenHash
            )
    }
}
