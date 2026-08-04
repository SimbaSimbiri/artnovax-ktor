package com.simbiri.presentation.routes

import com.simbiri.domain.model.auth.AuthenticatedSession
import com.simbiri.domain.model.auth.AuthenticationError
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.config.configureSerialization
import com.simbiri.presentation.routes.dto.auth.AuthenticatedSessionResponseDto
import com.simbiri.presentation.routes.dto.auth.AuthenticationErrorResponseDto
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthenticationRoutesTest {

    @Test
    fun `login returns access token and clears route password copy`() = testApplication {
        val expectedUserId = UserId(
            UUID.randomUUID()
        )

        val expectedExpiry = Instant.parse(
            "2026-07-28T20:15:00Z"
        )

        val expectedAccessExpiry =
            Instant.parse(
                "2026-07-28T20:15:00Z"
            )

        val expectedRefreshExpiry =
            Instant.parse(
                "2026-08-27T20:00:00Z"
            )

        var receivedEmailAddress: String? = null

        var receivedPassword: CharArray? = null

        application {
            configureAuthenticationRouteTest {
                    emailAddress,
                    password,
                ->

                receivedEmailAddress = emailAddress

                /*
                 * Retain the reference so the test can verify that the
                 * route clears the exact array supplied to the handler.
                 */
                receivedPassword = password

                ResultType.Success(
                    AuthenticatedSession(
                        userId = expectedUserId,
                        accessToken = "signed-test-token",
                        tokenType = "Bearer",
                        accessTokenExpiresAt = expectedAccessExpiry,
                        refreshToken = "test-refresh-token",
                        refreshTokenExpiresAt = expectedRefreshExpiry,
                    )
                )
            }
        }

        val response = client.post("/auth/login") {
            contentType(
                ContentType.Application.Json
            )

            setBody(
                """
                {
                    "emailAddress": "user@example.com",
                    "password": "123456789012345"
                }
                """.trimIndent()
            )
        }

        assertEquals(
            expected = HttpStatusCode.OK,
            actual = response.status,
        )

        assertEquals(
            expected = "no-store",
            actual = response.headers[HttpHeaders.CacheControl],
        )

        assertEquals(
            expected = "no-cache",
            actual = response.headers[HttpHeaders.Pragma],
        )

        val responseDto = Json.decodeFromString<AuthenticatedSessionResponseDto>(
            response.bodyAsText()
        )

        assertEquals(
            expected = expectedUserId.value.toString(),
            actual = responseDto.userId,
        )

        assertEquals(
            expected = "signed-test-token",
            actual = responseDto.accessToken,
        )

        assertEquals(
            expected = "Bearer",
            actual = responseDto.tokenType,
        )

        assertEquals(
            expected = expectedAccessExpiry.toString(),
            actual = responseDto.accessTokenExpiresAt,
        )

        assertEquals(
            expected = "test-refresh-token",
            actual = responseDto.refreshToken,
        )

        assertEquals(
            expected = expectedRefreshExpiry.toString(),
            actual = responseDto.refreshTokenExpiresAt,
        )

        assertEquals(
            expected = "user@example.com",
            actual = receivedEmailAddress,
        )

        val clearedPassword = assertNotNull(
            receivedPassword
        )

        assertTrue(
            clearedPassword.all { character ->
                character == '\u0000'
            })
    }

    @Test
    fun `login returns generic unauthorized response for invalid credentials`() = testApplication {
        application {
            configureAuthenticationRouteTest {
                    _,
                    _,
                ->

                ResultType.Failure(
                    AuthenticationError.InvalidCredentials
                )
            }
        }

        val response = postLoginRequest()

        assertGenericUnauthorizedResponse(
            status = response.status,
            body = response.bodyAsText(),
        )
    }

    @Test
    fun `login does not expose temporary account lock`() = testApplication {
        application {
            configureAuthenticationRouteTest {
                    _,
                    _,
                ->

                ResultType.Failure(
                    AuthenticationError.TemporarilyLocked
                )
            }
        }

        val response = postLoginRequest()

        assertGenericUnauthorizedResponse(
            status = response.status,
            body = response.bodyAsText(),
        )
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.postLoginRequest() = client.post("/auth/login") {
        contentType(
            ContentType.Application.Json
        )

        setBody(
            """
            {
                "emailAddress": "user@example.com",
                "password": "incorrect-password"
            }
            """.trimIndent()
        )
    }

    private fun assertGenericUnauthorizedResponse(
        status: HttpStatusCode,
        body: String,
    ) {
        assertEquals(
            expected = HttpStatusCode.Unauthorized,
            actual = status,
        )

        val errorResponse = Json.decodeFromString<AuthenticationErrorResponseDto>(body)

        assertEquals(
            expected = "INVALID_CREDENTIALS",
            actual = errorResponse.code,
        )

        assertEquals(
            expected = "Email address or password is incorrect.",
            actual = errorResponse.message,
        )
    }
}

/**
 * Installs only the infrastructure required to exercise the login route.
 */
private fun io.ktor.server.application.Application.configureAuthenticationRouteTest(
    authenticateUser: AuthenticateUserHandler,
) {
    install(Resources)

    configureSerialization()

    routing {
        authenticationRoutes(
            authenticateUser = authenticateUser,
        )
    }
}
