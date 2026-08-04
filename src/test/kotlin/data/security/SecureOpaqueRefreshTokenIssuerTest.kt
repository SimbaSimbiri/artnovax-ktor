package com.simbiri.data.security

import com.simbiri.domain.security.RefreshTokenSettings
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SecureOpaqueRefreshTokenIssuerTest {

    private val issuedAt = Instant.parse(
        "2026-08-04T19:00:00Z"
    )

    private val issuer = SecureOpaqueRefreshTokenIssuer(
        settings = RefreshTokenSettings(
            ttlSeconds = 3_600L
        ),
        clock = Clock.fixed(
            issuedAt,
            ZoneOffset.UTC,
        ),
    )

    @Test
    fun `issues URL-safe 256-bit token and SHA-256 digest`() {
        val token = issuer.issue()

        assertEquals(
            expected = 43,
            actual = token.value.length,
        )

        assertTrue(
            token.value.matches(
                Regex(
                    "^[A-Za-z0-9_-]{43}$"
                )
            )
        )

        assertEquals(
            expected = 64,
            actual = token.hash.length,
        )

        assertTrue(
            token.hash.matches(
                Regex("^[0-9a-f]{64}$")
            )
        )

        assertEquals(
            expected = token.hash,
            actual = issuer.hash(token.value),
        )

        assertEquals(
            expected = issuedAt.plusSeconds(3_600L),
            actual = token.expiresAt,
        )
    }

    @Test
    fun `successive token issuance produces different values`() {
        val firstToken = issuer.issue()

        val secondToken = issuer.issue()

        assertNotEquals(
            illegal = firstToken.value,
            actual = secondToken.value,
        )

        assertNotEquals(
            illegal = firstToken.hash,
            actual = secondToken.hash,
        )
    }

    @Test
    fun `rejects malformed token before hashing`() {
        assertFailsWith<IllegalArgumentException> {
            issuer.hash(
                "not-a-valid-refresh-token"
            )
        }
    }
}
