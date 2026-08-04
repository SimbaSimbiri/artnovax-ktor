package com.simbiri.application.auth

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.repository.AccessTokenSessionRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidateAccessTokenSessionUseCaseTest {

    @Test
    fun `accepts current persisted session`() = runBlocking {
        val repository = AccessTokenSessionRepositoryFake(
            result = ResultType.Success(true)
        )

        val useCase = ValidateAccessTokenSessionUseCase(
            accessTokenSessionRepository = repository
        )

        assertTrue(
            useCase(
                userId = UserId(
                    UUID.randomUUID()
                ),
                sessionVersion = 2L,
            )
        )
    }

    @Test
    fun `rejects stale persisted session`() = runBlocking {
        val repository = AccessTokenSessionRepositoryFake(
            result = ResultType.Success(false)
        )

        val useCase = ValidateAccessTokenSessionUseCase(
            accessTokenSessionRepository = repository
        )

        assertFalse(
            useCase(
                userId = UserId(
                    UUID.randomUUID()
                ),
                sessionVersion = 1L,
            )
        )
    }

    @Test
    fun `rejects session when persistence fails`() = runBlocking {
        val repository = AccessTokenSessionRepositoryFake(
            result = ResultType.Failure(
                DataError.UnknownError(
                    cause = "Database unavailable."
                )
            )
        )

        val useCase = ValidateAccessTokenSessionUseCase(
            accessTokenSessionRepository = repository
        )

        assertFalse(
            useCase(
                userId = UserId(
                    UUID.randomUUID()
                ),
                sessionVersion = 1L,
            )
        )
    }

    @Test
    fun `rejects invalid version without repository access`() = runBlocking {
        val repository = AccessTokenSessionRepositoryFake(
            result = ResultType.Success(true)
        )

        val useCase = ValidateAccessTokenSessionUseCase(
            accessTokenSessionRepository = repository
        )

        assertFalse(
            useCase(
                userId = UserId(
                    UUID.randomUUID()
                ),
                sessionVersion = 0L,
            )
        )

        assertTrue(
            repository.receivedRequests.isEmpty()
        )
    }
}

private class AccessTokenSessionRepositoryFake(
    private val result: ResultType<Boolean, DataError>,
) : AccessTokenSessionRepository {

    val receivedRequests = mutableListOf<Pair<UserId, Long>>()

    override suspend fun isCurrent(
        userId: UserId,
        sessionVersion: Long,
    ): ResultType<Boolean, DataError> {
        receivedRequests += userId to sessionVersion

        return result
    }
}
