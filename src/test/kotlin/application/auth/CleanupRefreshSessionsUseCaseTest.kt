package com.simbiri.application.auth

import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.repository.RefreshSessionCleanupRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import kotlinx.coroutines.runBlocking
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CleanupRefreshSessionsUseCaseTest {

    @Test
    fun `deletes sessions in bounded batches`() = runBlocking {
        val repository = RefreshSessionCleanupRepositoryFake(
            results = listOf(
                ResultType.Success(500),
                ResultType.Success(500),
                ResultType.Success(73),
            )
        )

        val useCase = CleanupRefreshSessionsUseCase(
            refreshSessionCleanupRepository = repository
        )

        val result = useCase(
            expiredBefore = CUTOFF,
            batchSize = 500,
            maxBatches = 20,
        )

        val success = assertIs<ResultType.Success<RefreshSessionCleanupReport>>(result)

        assertEquals(
            expected = 1_073,
            actual = success.data.deletedCount,
        )

        assertEquals(
            expected = 3,
            actual = success.data.processedBatches,
        )

        assertEquals(
            expected = 3,
            actual = repository.requests.size,
        )
    }

    @Test
    fun `stops after maximum batch count`() = runBlocking {
        val repository = RefreshSessionCleanupRepositoryFake(
            results = listOf(
                ResultType.Success(500),
                ResultType.Success(500),
                ResultType.Success(500),
            )
        )

        val useCase = CleanupRefreshSessionsUseCase(
            refreshSessionCleanupRepository = repository
        )

        val result = useCase(
            expiredBefore = CUTOFF,
            batchSize = 500,
            maxBatches = 2,
        )

        val success = assertIs<ResultType.Success<RefreshSessionCleanupReport>>(result)

        assertEquals(
            expected = 1_000,
            actual = success.data.deletedCount,
        )

        assertEquals(
            expected = 2,
            actual = repository.requests.size,
        )
    }

    @Test
    fun `returns repository failure`() = runBlocking {
        val repositoryError = DataError.DatabaseError(
            operation = "deleteExpiredRefreshSessions",
            cause = "Database unavailable.",
        )

        val repository = RefreshSessionCleanupRepositoryFake(
            results = listOf(
                ResultType.Failure(
                    repositoryError
                )
            )
        )

        val useCase = CleanupRefreshSessionsUseCase(
            refreshSessionCleanupRepository = repository
        )

        val result = useCase(
            expiredBefore = CUTOFF,
            batchSize = 500,
            maxBatches = 20,
        )

        val failure = assertIs<ResultType.Failure<DataError>>(result)

        assertEquals(
            expected = repositoryError,
            actual = failure.error,
        )
    }

    private companion object {
        val CUTOFF: Instant = Instant.parse(
            "2026-07-28T00:00:00Z"
        )
    }
}

private data class CleanupRequest(
    val expiredBefore: Timestamp,
    val limit: Int,
)

private class RefreshSessionCleanupRepositoryFake(
    results: List<ResultType<Int, DataError>>,
) : RefreshSessionCleanupRepository {

    private val remainingResults = results.toMutableList()

    val requests = mutableListOf<CleanupRequest>()

    override suspend fun deleteSessionsExpiredBefore(
        expiredBefore: Timestamp,
        limit: Int,
    ): ResultType<Int, DataError> {
        requests += CleanupRequest(
            expiredBefore = expiredBefore,
            limit = limit,
        )

        return remainingResults.removeAt(0)
    }
}
