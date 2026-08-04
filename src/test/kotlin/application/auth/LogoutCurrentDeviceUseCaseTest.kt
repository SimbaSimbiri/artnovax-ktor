package com.simbiri.application.auth

import com.simbiri.domain.model.auth.RefreshSession
import com.simbiri.domain.model.auth.RefreshSessionRotationResult
import com.simbiri.domain.model.common.RefreshSessionId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.repository.RefreshSessionRepository
import com.simbiri.domain.security.RefreshTokenIssuer
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LogoutCurrentDeviceUseCaseTest {

    @Test
    fun `valid refresh token revokes its token family`() = runBlocking {
        val repository = LogoutRefreshSessionRepositoryFake(
            revocationResult = ResultType.Success(Unit)
        )

        val useCase = LogoutCurrentDeviceUseCase(
            refreshSessionRepository = repository,
            refreshTokenIssuer = LogoutRefreshTokenIssuerFake(),
        )

        val result = useCase(
            VALID_REFRESH_TOKEN
        )

        assertIs<ResultType.Success<Unit>>(result)

        assertEquals(
            expected = listOf(VALID_TOKEN_HASH),
            actual = repository.revokedHashes,
        )
    }

    @Test
    fun `malformed refresh token is successful no-op`() = runBlocking {
        val repository = LogoutRefreshSessionRepositoryFake(
            revocationResult = ResultType.Success(Unit)
        )

        val useCase = LogoutCurrentDeviceUseCase(
            refreshSessionRepository = repository,
            refreshTokenIssuer = LogoutRefreshTokenIssuerFake(),
        )

        val result = useCase(
            "malformed-token"
        )

        assertIs<ResultType.Success<Unit>>(result)

        assertTrue(
            repository.revokedHashes.isEmpty()
        )
    }

    @Test
    fun `repository failure is returned`() = runBlocking {
        val repositoryError = DataError.DatabaseError(
            operation = "revokeRefreshSessionFamily",
            cause = "Database unavailable.",
        )

        val repository = LogoutRefreshSessionRepositoryFake(
            revocationResult = ResultType.Failure(
                repositoryError
            )
        )

        val useCase = LogoutCurrentDeviceUseCase(
            refreshSessionRepository = repository,
            refreshTokenIssuer = LogoutRefreshTokenIssuerFake(),
        )

        val result = useCase(
            VALID_REFRESH_TOKEN
        )

        val failure = assertIs<ResultType.Failure<DataError>>(result)

        assertEquals(
            expected = repositoryError,
            actual = failure.error,
        )
    }

    private companion object {
        const val VALID_REFRESH_TOKEN = "valid-refresh-token"

        val VALID_TOKEN_HASH = "c".repeat(64)
    }
}

private class LogoutRefreshTokenIssuerFake : RefreshTokenIssuer {

    override fun issue() = error(
        "issue is not used by this test."
    )

    override fun hash(
        tokenValue: String,
    ): String {
        if (tokenValue != "valid-refresh-token") {
            throw IllegalArgumentException(
                "Malformed refresh token."
            )
        }

        return "c".repeat(64)
    }
}

private class LogoutRefreshSessionRepositoryFake(
    private val revocationResult: ResultType<Unit, DataError>,
) : RefreshSessionRepository {

    val revokedHashes = mutableListOf<String>()

    override suspend fun revokeFamilyByTokenHash(
        tokenHash: String,
    ): ResultType<Unit, DataError> {
        revokedHashes += tokenHash

        return revocationResult
    }

    override suspend fun createSession(
        session: RefreshSession,
    ): ResultType<
            RefreshSessionId,
            DataError,
            > = error("Not used by this test.")

    override suspend fun getSessionByTokenHash(
        tokenHash: String,
    ): ResultType<
            RefreshSession,
            DataError,
            > = error("Not used by this test.")

    override suspend fun rotateSession(
        presentedTokenHash: String,
        replacementTokenHash: String,
        replacementExpiresAt: Timestamp,
    ): ResultType<
            RefreshSessionRotationResult,
            DataError,
            > = error("Not used by this test.")
}
