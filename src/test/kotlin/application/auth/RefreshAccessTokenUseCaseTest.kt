package com.simbiri.application.auth

import com.simbiri.domain.model.auth.AuthenticatedSession
import com.simbiri.domain.model.auth.IssuedAccessToken
import com.simbiri.domain.model.auth.IssuedRefreshToken
import com.simbiri.domain.model.auth.RefreshAuthenticationError
import com.simbiri.domain.model.auth.RefreshSession
import com.simbiri.domain.model.auth.RefreshSessionRotationResult
import com.simbiri.domain.model.common.RefreshSessionId
import com.simbiri.domain.model.common.RefreshTokenFamilyId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.repository.RefreshSessionRepository
import com.simbiri.domain.security.AccessTokenIssuer
import com.simbiri.domain.security.RefreshTokenIssuer
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import kotlinx.coroutines.runBlocking
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RefreshAccessTokenUseCaseTest {

    private val clock = Clock.fixed(
        NOW,
        ZoneOffset.UTC,
    )

    @Test
    fun `active refresh token rotates and returns replacement session`() = runBlocking {
        val currentSession = activeRefreshSession()

        val replacementSessionId = RefreshSessionId(
            UUID.randomUUID()
        )

        val repository = RefreshSessionRepositoryFake(
            lookupResult = ResultType.Success(
                currentSession
            ),

            rotationResult = ResultType.Success(
                RefreshSessionRotationResult.Rotated(
                    refreshSessionId = replacementSessionId,
                    userId = currentSession.userId,
                    sessionVersion = currentSession.sessionVersion,
                )
            ),
        )

        val refreshTokenIssuer = RefreshTokenIssuerFake()

        val accessTokenIssuer = AccessTokenIssuerFake()

        val useCase = RefreshAccessTokenUseCase(
            refreshSessionRepository = repository,
            refreshTokenIssuer = refreshTokenIssuer,
            accessTokenIssuer = accessTokenIssuer,
            clock = clock,
        )

        val result = useCase(
            PRESENTED_REFRESH_TOKEN
        )

        val success = assertIs<ResultType.Success<AuthenticatedSession>>(result)

        val authenticatedSession = success.data

        assertEquals(
            expected = currentSession.userId,
            actual = authenticatedSession.userId,
        )

        assertEquals(
            expected = ISSUED_ACCESS_TOKEN,
            actual = authenticatedSession.accessToken,
        )

        assertEquals(
            expected = "Bearer",
            actual = authenticatedSession.tokenType,
        )

        assertEquals(
            expected = ACCESS_TOKEN_EXPIRY,
            actual = authenticatedSession.accessTokenExpiresAt,
        )

        assertEquals(
            expected = REPLACEMENT_REFRESH_TOKEN,
            actual = authenticatedSession.refreshToken,
        )

        assertEquals(
            expected = REPLACEMENT_REFRESH_TOKEN_EXPIRY,
            actual = authenticatedSession.refreshTokenExpiresAt,
        )

        assertEquals(
            expected = listOf(CURRENT_TOKEN_HASH),
            actual = repository.lookupHashes,
        )

        assertEquals(
            expected = 1,
            actual = repository.rotationRequests.size,
        )

        val rotationRequest = repository.rotationRequests.single()

        assertEquals(
            expected = CURRENT_TOKEN_HASH,
            actual = rotationRequest.presentedTokenHash,
        )

        assertEquals(
            expected = REPLACEMENT_TOKEN_HASH,
            actual = rotationRequest.replacementTokenHash,
        )

        assertEquals(
            expected = REPLACEMENT_REFRESH_TOKEN_EXPIRY,
            actual = rotationRequest.replacementExpiresAt,
        )

        assertEquals(
            expected = 1,
            actual = refreshTokenIssuer.issueCalls,
        )

        assertEquals(
            expected = 1,
            actual = accessTokenIssuer.issueCalls,
        )

        assertEquals(
            expected = currentSession.userId,
            actual = accessTokenIssuer.receivedUserId,
        )

        assertEquals(
            expected = currentSession.sessionVersion,
            actual = accessTokenIssuer.receivedSessionVersion,
        )
    }

    @Test
    fun `unknown refresh token returns invalid refresh token`() = runBlocking {
        val repository = RefreshSessionRepositoryFake(
            lookupResult = ResultType.Failure(
                DataError.NotFound
            ),
            rotationResult = ResultType.Success(
                RefreshSessionRotationResult.Invalid
            ),
        )

        val refreshTokenIssuer = RefreshTokenIssuerFake()

        val accessTokenIssuer = AccessTokenIssuerFake()

        val useCase = RefreshAccessTokenUseCase(
            refreshSessionRepository = repository,
            refreshTokenIssuer = refreshTokenIssuer,
            accessTokenIssuer = accessTokenIssuer,
            clock = clock,
        )

        val result = useCase(
            PRESENTED_REFRESH_TOKEN
        )

        val failure = assertIs<ResultType.Failure<RefreshAuthenticationError>>(result)

        assertEquals(
            expected = RefreshAuthenticationError.InvalidRefreshToken,

            actual = failure.error,
        )

        assertTrue(
            repository.rotationRequests.isEmpty()
        )

        assertEquals(
            expected = 0,
            actual = refreshTokenIssuer.issueCalls,
        )

        assertEquals(
            expected = 0,
            actual = accessTokenIssuer.issueCalls,
        )
    }

    @Test
    fun `malformed refresh token returns invalid refresh token`() = runBlocking {
        val repository = RefreshSessionRepositoryFake(
            lookupResult = ResultType.Success(
                activeRefreshSession()
            ),
            rotationResult = ResultType.Success(
                RefreshSessionRotationResult.Invalid
            ),
        )

        val refreshTokenIssuer = RefreshTokenIssuerFake()

        val accessTokenIssuer = AccessTokenIssuerFake()

        val useCase = RefreshAccessTokenUseCase(
            refreshSessionRepository = repository,
            refreshTokenIssuer = refreshTokenIssuer,
            accessTokenIssuer = accessTokenIssuer,
            clock = clock,
        )

        val result = useCase(
            "malformed-token"
        )

        val failure = assertIs<ResultType.Failure<RefreshAuthenticationError>>(result)

        assertEquals(
            expected = RefreshAuthenticationError.InvalidRefreshToken,

            actual = failure.error,
        )

        assertTrue(
            repository.lookupHashes.isEmpty()
        )

        assertTrue(
            repository.rotationRequests.isEmpty()
        )
    }

    @Test
    fun `expired refresh token returns invalid refresh token`() = runBlocking {
        val expiredSession = activeRefreshSession().copy(
                expiresAt = NOW.minusSeconds(1)
            )

        val repository = RefreshSessionRepositoryFake(
            lookupResult = ResultType.Success(
                expiredSession
            ),
            rotationResult = ResultType.Success(
                RefreshSessionRotationResult.Invalid
            ),
        )

        val refreshTokenIssuer = RefreshTokenIssuerFake()

        val accessTokenIssuer = AccessTokenIssuerFake()

        val useCase = RefreshAccessTokenUseCase(
            refreshSessionRepository = repository,
            refreshTokenIssuer = refreshTokenIssuer,
            accessTokenIssuer = accessTokenIssuer,
            clock = clock,
        )

        val result = useCase(
            PRESENTED_REFRESH_TOKEN
        )

        val failure = assertIs<ResultType.Failure<RefreshAuthenticationError>>(result)

        assertEquals(
            expected = RefreshAuthenticationError.InvalidRefreshToken,
            actual = failure.error,
        )

        /*
         * The replacement refresh token is generated before the
         * repository performs its authoritative locked validation.
         * It is never returned when rotation fails.
         */
        assertEquals(
            expected = 1,
            actual = refreshTokenIssuer.issueCalls,
        )

        /*
         * No access token should be created from a session that is
         * already known to be expired.
         */
        assertEquals(
            expected = 0,
            actual = accessTokenIssuer.issueCalls,
        )
    }

    @Test
    fun `revoked token reuse returns invalid refresh token`() = runBlocking {
        val revokedSession = activeRefreshSession().copy(
                revokedAt = NOW.minusSeconds(30)
            )
        val repository = RefreshSessionRepositoryFake(
            lookupResult = ResultType.Success(
                revokedSession
            ),
            rotationResult = ResultType.Success(
                RefreshSessionRotationResult.ReuseDetected
            ),
        )

        val refreshTokenIssuer = RefreshTokenIssuerFake()

        val accessTokenIssuer = AccessTokenIssuerFake()

        val useCase = RefreshAccessTokenUseCase(
            refreshSessionRepository = repository,
            refreshTokenIssuer = refreshTokenIssuer,
            accessTokenIssuer = accessTokenIssuer,
            clock = clock,
        )

        val result = useCase(
            PRESENTED_REFRESH_TOKEN
        )

        val failure = assertIs<ResultType.Failure<RefreshAuthenticationError>>(result)

        assertEquals(
            expected = RefreshAuthenticationError.InvalidRefreshToken,
            actual = failure.error,
        )

        /*
         * Reuse detection is intentionally hidden from the external
         * caller.
         */
        assertEquals(
            expected = 0,
            actual = accessTokenIssuer.issueCalls,
        )
    }

    @Test
    fun `rotation reuse result returns generic invalid refresh token`() = runBlocking {
        val currentSession = activeRefreshSession()

        val repository = RefreshSessionRepositoryFake(
            lookupResult = ResultType.Success(
                currentSession
            ),
            rotationResult = ResultType.Success(
                RefreshSessionRotationResult.ReuseDetected
            ),
        )

        val useCase = RefreshAccessTokenUseCase(
            refreshSessionRepository = repository,
            refreshTokenIssuer = RefreshTokenIssuerFake(),
            accessTokenIssuer = AccessTokenIssuerFake(),
            clock = clock,
        )

        val result = useCase(
            PRESENTED_REFRESH_TOKEN
        )

        val failure = assertIs<ResultType.Failure<RefreshAuthenticationError>>(result)

        assertEquals(
            expected = RefreshAuthenticationError.InvalidRefreshToken,
            actual = failure.error,
        )
    }

    @Test
    fun `rotation repository failure returns data failure and no session`() = runBlocking {
        val repositoryError = DataError.DatabaseError(
            operation = "rotateRefreshSession",
            cause = "Database unavailable.",
        )

        val repository = RefreshSessionRepositoryFake(
            lookupResult = ResultType.Success(
                activeRefreshSession()
            ),
            rotationResult = ResultType.Failure(
                repositoryError
            ),
        )

        val refreshTokenIssuer = RefreshTokenIssuerFake()

        val accessTokenIssuer = AccessTokenIssuerFake()

        val useCase = RefreshAccessTokenUseCase(
            refreshSessionRepository = repository,
            refreshTokenIssuer = refreshTokenIssuer,
            accessTokenIssuer = accessTokenIssuer,
            clock = clock,
        )

        val result = useCase(
            PRESENTED_REFRESH_TOKEN
        )

        val failure = assertIs<ResultType.Failure<RefreshAuthenticationError>>(result)

        val dataFailure = assertIs<RefreshAuthenticationError.DataFailure>(failure.error)

        assertEquals(
            expected = repositoryError,

            actual = dataFailure.error,
        )

        /*
         * Both replacements may exist only in local memory, but no
         * AuthenticatedSession containing them is returned.
         */
        assertEquals(
            expected = 1,
            actual = refreshTokenIssuer.issueCalls,
        )

        assertEquals(
            expected = 1,
            actual = accessTokenIssuer.issueCalls,
        )
    }

    @Test
    fun `rotation identity mismatch returns data failure`() : Unit = runBlocking {
        val currentSession = activeRefreshSession()

        val repository = RefreshSessionRepositoryFake(
            lookupResult = ResultType.Success(
                currentSession
            ),

            rotationResult = ResultType.Success(
                RefreshSessionRotationResult.Rotated(
                    refreshSessionId = RefreshSessionId(
                        UUID.randomUUID()
                    ),
                    userId = UserId(
                        UUID.randomUUID()
                    ),
                    sessionVersion = currentSession.sessionVersion,
                )
            ),
        )

        val useCase = RefreshAccessTokenUseCase(
            refreshSessionRepository = repository,
            refreshTokenIssuer = RefreshTokenIssuerFake(),
            accessTokenIssuer = AccessTokenIssuerFake(),
            clock = clock,
        )

        val result = useCase(
            PRESENTED_REFRESH_TOKEN
        )

        val failure = assertIs<ResultType.Failure<RefreshAuthenticationError>>(result)

        assertIs<RefreshAuthenticationError.DataFailure>(failure.error)
    }

    private fun activeRefreshSession(): RefreshSession = RefreshSession(
        id = RefreshSessionId(
            UUID.randomUUID()
        ),
        familyId = RefreshTokenFamilyId(
            UUID.randomUUID()
        ),
        userId = UserId(
            UUID.randomUUID()
        ),
        tokenHash = CURRENT_TOKEN_HASH,
        sessionVersion = 4L,
        expiresAt = NOW.plusSeconds(
            3_600L
        ),
        revokedAt = null,
        createdAt = NOW.minusSeconds(
            60L
        ),
        updatedAt = NOW.minusSeconds(
            60L
        ),
    )

    private companion object {
        val NOW: Instant = Instant.parse(
            "2026-08-04T20:00:00Z"
        )

        val ACCESS_TOKEN_EXPIRY: Instant = NOW.plusSeconds(
            15L * 60L
        )

        val REPLACEMENT_REFRESH_TOKEN_EXPIRY: Instant = NOW.plusSeconds(
            30L * 24L * 60L * 60L
        )

        const val PRESENTED_REFRESH_TOKEN = "presented-refresh-token"

        const val REPLACEMENT_REFRESH_TOKEN = "replacement-refresh-token"

        const val ISSUED_ACCESS_TOKEN = "signed-replacement-access-token"

        val CURRENT_TOKEN_HASH: String = "a".repeat(64)

        val REPLACEMENT_TOKEN_HASH: String = "b".repeat(64)
    }
}

/**
 * Captures one requested refresh-session rotation.
 */
private data class RotationRequest(
    val presentedTokenHash: String,
    val replacementTokenHash: String,
    val replacementExpiresAt: Timestamp,
)

/**
 * Configurable refresh-session repository fake.
 */
private class RefreshSessionRepositoryFake(
    private val lookupResult: ResultType<RefreshSession, DataError>,

    private val rotationResult: ResultType<RefreshSessionRotationResult, DataError>,
) : RefreshSessionRepository {

    val lookupHashes = mutableListOf<String>()
    val rotationRequests = mutableListOf<RotationRequest>()

    override suspend fun createSession(
        session: RefreshSession,
    ): ResultType<RefreshSessionId, DataError> = error(
        "createSession is not used by this test."
    )

    override suspend fun getSessionByTokenHash(
        tokenHash: String,
    ): ResultType<RefreshSession, DataError> {
        lookupHashes += tokenHash

        return lookupResult
    }

    override suspend fun rotateSession(
        presentedTokenHash: String,
        replacementTokenHash: String,
        replacementExpiresAt: Timestamp,
    ): ResultType<RefreshSessionRotationResult, DataError> {
        rotationRequests += RotationRequest(
            presentedTokenHash = presentedTokenHash,
            replacementTokenHash = replacementTokenHash,
            replacementExpiresAt = replacementExpiresAt,
        )

        return rotationResult
    }
}

/**
 * Deterministic opaque refresh-token issuer fake.
 */
private class RefreshTokenIssuerFake : RefreshTokenIssuer {

    var issueCalls: Int = 0
        private set

    override fun issue(): IssuedRefreshToken {
        issueCalls += 1

        return IssuedRefreshToken(
            value = "replacement-refresh-token",
            hash = "b".repeat(64),
            expiresAt = Instant.parse(
                "2026-09-03T20:00:00Z"
            ),
        )
    }

    override fun hash(
        tokenValue: String,
    ): String {
        if (tokenValue != "presented-refresh-token") {
            throw IllegalArgumentException(
                "Invalid refresh-token format."
            )
        }

        return "a".repeat(64)
    }
}

/**
 * Deterministic access-token issuer fake.
 */
private class AccessTokenIssuerFake : AccessTokenIssuer {

    var issueCalls: Int = 0
        private set

    var receivedUserId: UserId? = null
        private set

    var receivedSessionVersion: Long? = null
        private set

    override fun issue(
        userId: UserId,
        sessionVersion: Long,
    ): IssuedAccessToken {
        issueCalls += 1
        receivedUserId = userId
        receivedSessionVersion = sessionVersion

        return IssuedAccessToken(
            value = "signed-replacement-access-token",
            expiresAt = Instant.parse(
                "2026-08-04T20:15:00Z"
            ),
        )
    }
}
