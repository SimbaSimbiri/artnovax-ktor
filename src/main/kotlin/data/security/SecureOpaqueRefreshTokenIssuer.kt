package com.simbiri.data.security

import com.simbiri.domain.model.auth.IssuedRefreshToken
import com.simbiri.domain.security.RefreshTokenIssuer
import com.simbiri.domain.security.RefreshTokenSettings
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.Base64

/**
 * Generates 256-bit opaque refresh tokens using SecureRandom.
 *
 * Tokens use unpadded Base64 URL encoding and therefore contain only
 * URL-safe characters.
 */
class SecureOpaqueRefreshTokenIssuer(
    private val settings: RefreshTokenSettings,
    private val clock: Clock,
    private val secureRandom: SecureRandom = SecureRandom(),
) : RefreshTokenIssuer {

    override fun issue(): IssuedRefreshToken {
        val randomBytes = ByteArray(TOKEN_BYTE_COUNT)

        secureRandom.nextBytes(randomBytes)

        val tokenValue = try {
            Base64.getUrlEncoder().withoutPadding().encodeToString(
                    randomBytes
                )
        } finally {
            randomBytes.fill(0)
        }

        val issuedAt = Instant.now(clock)

        return IssuedRefreshToken(
            value = tokenValue,
            hash = hash(tokenValue),
            expiresAt = issuedAt.plusSeconds(
                settings.ttlSeconds
            ),
        )
    }

    override fun hash(
        tokenValue: String,
    ): String {
        require(
            RAW_TOKEN_PATTERN.matches(tokenValue)
        ) {
            "Refresh token has an invalid format."
        }

        val tokenBytes = tokenValue.toByteArray(StandardCharsets.US_ASCII)

        val digest = try {
            MessageDigest.getInstance("SHA-256").digest(tokenBytes)
        } finally {
            tokenBytes.fill(0)
        }

        return try {
            buildString(
                digest.size * 2
            ) {
                digest.forEach { byte ->
                    append(
                        (byte.toInt() and 0xFF).toString(16).padStart(
                                length = 2,
                                padChar = '0',
                            )
                    )
                }
            }
        } finally {
            digest.fill(0)
        }
    }

    private companion object {
        const val TOKEN_BYTE_COUNT = 32

        /*
         * Thirty-two bytes encoded as unpadded Base64 URL text should produce forty-three characters.
         */
        val RAW_TOKEN_PATTERN = Regex("^[A-Za-z0-9_-]{43}$")
    }
}
