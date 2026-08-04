package com.simbiri.support.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.simbiri.presentation.auth.JWT_SESSION_VERSION_CLAIM
import com.simbiri.presentation.auth.JwtSettings
import java.time.Instant
import java.util.Date
import java.util.UUID

/**
 * Creates signed JWT access tokens for authentication tests.
 *
 * This factory is test-only. Production token issuance will belong to the
 * authentication application layer rather than presentation.
 */
object TestJwtTokenFactory {

    fun createAccessToken(
        settings: JwtSettings,
        subject: String =
            UUID.randomUUID().toString(),
        issuer: String = settings.issuer,
        audience: String = settings.audience,
        secret: String = settings.secret,
        issuedAt: Instant = Instant.now(),
        sessionVersion: Long? = DEFAULT_SESSION_VERSION,
        expiresAt: Instant =
            issuedAt.plusSeconds(
                DEFAULT_TOKEN_TTL_SECONDS
            ),
        notBefore: Instant? = null,
    ): String {
        val tokenBuilder =
            JWT
                .create()
                .withIssuer(issuer)
                .withAudience(audience)
                .withSubject(subject)
                .withIssuedAt(
                    Date.from(issuedAt)
                )
                .withExpiresAt(
                    Date.from(expiresAt)
                )

        /*
         * notBefore remains optional so individual tests can construct
         * tokens that are validly signed but not yet usable.
         */
        if (notBefore != null) {
            tokenBuilder.withNotBefore(
                Date.from(notBefore)
            )
        }

        /*
         * Null allows tests to create a correctly signed token that is missing the
         * required server-side session claim.
         */
        if (sessionVersion != null) {
            tokenBuilder.withClaim(
                JWT_SESSION_VERSION_CLAIM,
                sessionVersion,
            )
        }

        return tokenBuilder.sign(
            Algorithm.HMAC256(secret)
        )
    }

    private const val DEFAULT_TOKEN_TTL_SECONDS =
        5L * 60L
    private const val DEFAULT_SESSION_VERSION = 1L
}
