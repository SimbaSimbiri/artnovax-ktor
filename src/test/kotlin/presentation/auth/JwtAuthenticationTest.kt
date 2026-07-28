package com.simbiri.presentation.auth

import com.simbiri.presentation.config.JWT_AUTH_PROVIDER
import com.simbiri.presentation.config.configureAuthentication
import com.simbiri.presentation.config.configureSerialization
import com.simbiri.support.auth.TestJwtTokenFactory
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import java.time.Instant
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class JwtAuthenticationTest {

    @Test
    fun `protected route rejects request without bearer token`() = testApplication {
        application {
            configureJwtTestApplication()
        }

        val response = client.get(PROTECTED_PATH)

        val responseBody = response.bodyAsText()

        assertEquals(
            expected = HttpStatusCode.Unauthorized,
            actual = response.status,
            message = "body=$responseBody",
        )
    }

    @Test
    fun `protected route accepts valid bearer token`() = testApplication {
        application {
            configureJwtTestApplication()
        }

        val expectedUserId = UUID.randomUUID()

        val token = TestJwtTokenFactory.createAccessToken(
            settings = TEST_JWT_SETTINGS,
            subject = expectedUserId.toString(),
        )

        val response = client.get(PROTECTED_PATH) {
            bearerToken(token)
        }

        assertEquals(
            expected = HttpStatusCode.OK,
            actual = response.status,
        )

        assertEquals(
            expected = expectedUserId.toString(),
            actual = response.bodyAsText(),
        )
    }

    @Test
    fun `protected route rejects expired token`() = testApplication {
        application {
            configureJwtTestApplication()
        }

        val issuedAt = Instant.now().minusSeconds(10L * 60L)

        val token = TestJwtTokenFactory.createAccessToken(
            settings = TEST_JWT_SETTINGS,
            issuedAt = issuedAt,
            expiresAt = issuedAt.plusSeconds(
                5L * 60L
            ),
        )

        val response = client.get(PROTECTED_PATH) {
            bearerToken(token)
        }

        assertUnauthorized(response.status)
    }

    @Test
    fun `protected route rejects token with incorrect issuer`() = testApplication {
        application {
            configureJwtTestApplication()
        }

        val token = TestJwtTokenFactory.createAccessToken(
            settings = TEST_JWT_SETTINGS,
            issuer = "untrusted-issuer",
        )

        val response = client.get(PROTECTED_PATH) {
            bearerToken(token)
        }

        assertUnauthorized(response.status)
    }

    @Test
    fun `protected route rejects token with incorrect audience`() = testApplication {
        application {
            configureJwtTestApplication()
        }

        val token = TestJwtTokenFactory.createAccessToken(
            settings = TEST_JWT_SETTINGS,
            audience = "untrusted-client",
        )

        val response = client.get(PROTECTED_PATH) {
            bearerToken(token)
        }

        assertUnauthorized(response.status)
    }

    @Test
    fun `protected route rejects token signed with another secret`() = testApplication {
        application {
            configureJwtTestApplication()
        }

        val token = TestJwtTokenFactory.createAccessToken(
            settings = TEST_JWT_SETTINGS,
            secret = "another-test-secret-that-is-definitely-long-enough",
        )

        val response = client.get(PROTECTED_PATH) {
            bearerToken(token)
        }

        assertUnauthorized(response.status)
    }

    @Test
    fun `protected route rejects token with malformed user subject`() = testApplication {
        application {
            configureJwtTestApplication()
        }

        val token = TestJwtTokenFactory.createAccessToken(
            settings = TEST_JWT_SETTINGS,
            subject = "not-a-user-uuid",
        )

        val response = client.get(PROTECTED_PATH) {
            bearerToken(token)
        }

        assertUnauthorized(response.status)
    }

    @Test
    fun `protected route rejects token before not-before time`() = testApplication {
        application {
            configureJwtTestApplication()
        }

        val token = TestJwtTokenFactory.createAccessToken(
            settings = TEST_JWT_SETTINGS,
            notBefore = Instant.now().plusSeconds(5L * 60L),
        )

        val response = client.get(PROTECTED_PATH) {
            bearerToken(token)
        }

        assertUnauthorized(response.status)
    }

    private fun assertUnauthorized(
        actualStatus: HttpStatusCode,
    ) {
        assertEquals(expected = HttpStatusCode.Unauthorized, actual = actualStatus)
    }

}



/**
 * Installs only the infrastructure required to exercise JWT verification.
 */
private fun Application.configureJwtTestApplication() {
    configureSerialization()

    configureAuthentication(
        settings = TEST_JWT_SETTINGS,
    )

    routing {
        authenticate(JWT_AUTH_PROVIDER) {
            get("/test/protected") {
                val userId = requireNotNull(
                    call.authenticatedUserIdOrNull()
                ) {
                    "Authenticated route did not contain " + "an AuthenticatedUserPrincipal."
                }

                call.respondText(
                    userId.value.toString()
                )
            }
        }
    }
}


/**
 * Adds a bearer token to a test HTTP request.
 */
private fun HttpRequestBuilder.bearerToken(
    token: String,
) {
    header(
        key = HttpHeaders.Authorization,
        value = "Bearer $token",
    )
}


// TEST JWT Settings
private const val PROTECTED_PATH = "/test/protected"
private val TEST_JWT_SETTINGS = JwtSettings(
    secret = "artnovax-test-secret-with-more-than-thirty-two-bytes",
    issuer = "artnovax-test-api",
    audience = "artnovax-test-client",
    realm = "ArtNovaX Test",
)
