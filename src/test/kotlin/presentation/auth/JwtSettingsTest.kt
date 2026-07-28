package com.simbiri.presentation.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JwtSettingsTest {

    @Test
    fun `environment loader applies stable defaults`() {
        val settings = JwtSettings.fromEnvironment(
            environment = mapOf(
                "JWT_SECRET" to TEST_SECRET,
            )
        )

        assertEquals(
            expected = TEST_SECRET,
            actual = settings.secret,
        )

        assertEquals(
            expected = "artnovax-api",
            actual = settings.issuer,
        )

        assertEquals(
            expected = "artnovax-mobile",
            actual = settings.audience,
        )

        assertEquals(
            expected = "ArtNovaX",
            actual = settings.realm,
        )
    }

    @Test
    fun `environment loader accepts explicit overrides`() {
        val settings = JwtSettings.fromEnvironment(
            environment = mapOf(
                "JWT_SECRET" to TEST_SECRET,
                "JWT_ISSUER" to "test-issuer",
                "JWT_AUDIENCE" to "test-audience",
                "JWT_REALM" to "Test Realm",
            )
        )

        assertEquals(
            expected = "test-issuer",
            actual = settings.issuer,
        )

        assertEquals(
            expected = "test-audience",
            actual = settings.audience,
        )

        assertEquals(
            expected = "Test Realm",
            actual = settings.realm,
        )
    }

    @Test
    fun `environment loader rejects missing secret`() {
        assertFailsWith<IllegalStateException> {
            JwtSettings.fromEnvironment(
                environment = emptyMap()
            )
        }
    }

    @Test
    fun `settings reject secret shorter than minimum`() {
        assertFailsWith<IllegalArgumentException> {
            JwtSettings(
                secret = "too-short",
                issuer = "issuer",
                audience = "audience",
                realm = "realm",
            )
        }
    }

    companion object {

        private const val TEST_SECRET = "artnovax-test-secret-with-more-than-thirty-two-bytes"
    }
}
