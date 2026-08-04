package com.simbiri.presentation.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.simbiri.domain.model.auth.IssuedAccessToken
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.security.AccessTokenIssuer
import java.time.Clock
import java.time.Instant
import java.util.Date
import java.util.UUID

/**
 * Issues short-lived JWT access tokens.
 *
 * The subject contains only the persisted User UUID. Current user roles,
 * capabilities, and active status remain authoritative in PostgreSQL.
 */
class JwtAccessTokenIssuer(
    private val settings: JwtSettings,
    private val clock: Clock,
) : AccessTokenIssuer {

    private val signingAlgorithm = Algorithm.HMAC256(
        settings.secret
    )

    override fun issue(
        userId: UserId,
        sessionVersion: Long,
    ): IssuedAccessToken {
        require(sessionVersion > 0L) {
            "JWT session version must be positive."
        }
        val issuedAt = Instant.now(clock)

        val expiresAt = issuedAt.plusSeconds(
            settings.accessTokenTtlSeconds
        )

        val token =
            JWT.create()
                .withIssuer(
                    settings.issuer
                ).withAudience(
                    settings.audience
                ).withSubject(
                    userId.value.toString()
                ).withClaim(
                    JWT_SESSION_VERSION_CLAIM,
                    sessionVersion
                ).withIssuedAt(
                    Date.from(issuedAt)
                ).withNotBefore(
                    Date.from(issuedAt)
                ).withExpiresAt(
                    Date.from(expiresAt)
                ).withJWTId(
                    UUID.randomUUID().toString()
                ).sign(
                    signingAlgorithm
                )

        return IssuedAccessToken(
            value = token,
            expiresAt = expiresAt,
        )
    }
}
