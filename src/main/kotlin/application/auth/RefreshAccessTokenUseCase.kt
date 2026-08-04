package com.simbiri.application.auth

import com.simbiri.domain.model.auth.AuthenticatedSession
import com.simbiri.domain.model.auth.RefreshAuthenticationError
import com.simbiri.domain.model.auth.RefreshSessionRotationResult
import com.simbiri.domain.repository.RefreshSessionRepository
import com.simbiri.domain.security.AccessTokenIssuer
import com.simbiri.domain.security.RefreshTokenIssuer
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.time.Clock
import java.time.Instant

/**
 * Rotates an opaque refresh token and issues a new access token.
 *
 * The presented refresh token is consumed exactly once.
 */
class RefreshAccessTokenUseCase(
    private val refreshSessionRepository: RefreshSessionRepository,
    private val refreshTokenIssuer: RefreshTokenIssuer,
    private val accessTokenIssuer: AccessTokenIssuer,
    private val clock: Clock,
) {

    suspend operator fun invoke(
        refreshToken: String,
    ): ResultType<AuthenticatedSession, RefreshAuthenticationError> {
        val presentedTokenHash = try {
            refreshTokenIssuer.hash(
                refreshToken
            )
        } catch (_: Exception) {
            return invalidToken()
        }

        /*
         * This preliminary lookup provides the identity/version needed to
         * create the access token before the destructive rotation occurs.
         * The repository rechecks all state under a row lock.
         */
        val currentSession = when (val result = refreshSessionRepository.getSessionByTokenHash(
            presentedTokenHash
        )) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> if (result.error == DataError.NotFound) {
                return invalidToken()
            } else {
                return dataFailure(
                    result.error
                )
            }
        }

        val now = Instant.now(clock)

        val replacementRefreshToken = try {
            refreshTokenIssuer.issue()
        } catch (_: Exception) {
            return dataFailure(
                DataError.UnknownError(
                    cause = "Replacement refresh-token issuance failed."
                )
            )
        }

        /*
         * Issue in memory before consuming the existing refresh token. The
         * token is not returned unless the repository rotation succeeds.
         */
        val replacementAccessToken = if (currentSession.isActiveAt(now)) {
            try {
                accessTokenIssuer.issue(
                    userId = currentSession.userId,
                    sessionVersion = currentSession.sessionVersion,
                )
            } catch (_: Exception) {
                return dataFailure(
                    DataError.UnknownError(
                        cause = "Access-token issuance failed."
                    )
                )
            }
        } else {
            null
        }

        return when (val result = refreshSessionRepository.rotateSession(
            presentedTokenHash = presentedTokenHash,
            replacementTokenHash = replacementRefreshToken.hash,
            replacementExpiresAt = replacementRefreshToken.expiresAt,
        )) {
            is ResultType.Failure -> dataFailure(
                result.error
            )

            is ResultType.Success -> when (val rotation = result.data) {
                RefreshSessionRotationResult.Invalid,
                RefreshSessionRotationResult.ReuseDetected,
                    -> invalidToken()

                is RefreshSessionRotationResult.Rotated -> {
                    val accessToken = replacementAccessToken ?: return dataFailure(
                        DataError.UnknownError(
                            cause = "Refresh session rotated without an issued " + "access token."
                        )
                    )

                    /*
                     * Defensive consistency check between the preliminary
                     * read and authoritative locked transaction.
                     */
                    if (rotation.userId != currentSession.userId || rotation.sessionVersion != currentSession.sessionVersion) {
                        return dataFailure(
                            DataError.UnknownError(
                                cause = "Refresh-session identity changed during rotation."
                            )
                        )
                    }

                    ResultType.Success(
                        AuthenticatedSession(
                            userId = rotation.userId,
                            accessToken = accessToken.value,
                            tokenType = "Bearer",
                            accessTokenExpiresAt = accessToken.expiresAt,
                            refreshToken = replacementRefreshToken.value,
                            refreshTokenExpiresAt = replacementRefreshToken.expiresAt,
                        )
                    )
                }
            }
        }
    }

    private fun invalidToken(): ResultType<
            AuthenticatedSession,
            RefreshAuthenticationError,
            > = ResultType.Failure(
        RefreshAuthenticationError.InvalidRefreshToken
    )

    private fun dataFailure(
        error: DataError,
    ): ResultType<
            AuthenticatedSession,
            RefreshAuthenticationError,
            > = ResultType.Failure(
        RefreshAuthenticationError.DataFailure(error)
    )
}
