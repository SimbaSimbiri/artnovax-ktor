package com.simbiri.application.auth

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.repository.AccessTokenSessionCommandRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LogoutAllDevicesUseCaseTest {

    @Test
    fun `invalidates sessions belonging to authenticated user`() = runBlocking {
        val userId = UserId(
            UUID.randomUUID()
        )

        val repository = AccessTokenSessionCommandRepositoryFake(
            result = ResultType.Success(Unit)
        )

        val useCase = LogoutAllDevicesUseCase(
            accessTokenSessionCommandRepository = repository
        )

        val result = useCase(userId)

        assertIs<ResultType.Success<Unit>>(result)

        assertEquals(
            expected = listOf(userId),
            actual = repository.receivedUserIds,
        )
    }

    @Test
    fun `maps missing authenticated credential to server failure`(): Unit = runBlocking {
        val repository = AccessTokenSessionCommandRepositoryFake(
            result = ResultType.Failure(
                DataError.NotFound
            )
        )

        val useCase = LogoutAllDevicesUseCase(
            accessTokenSessionCommandRepository = repository
        )

        val result = useCase(
            UserId(
                UUID.randomUUID()
            )
        )

        val failure = assertIs<ResultType.Failure<DataError>>(result)

        assertIs<DataError.UnknownError>(failure.error)
    }
}

private class AccessTokenSessionCommandRepositoryFake(
    private val result: ResultType<Unit, DataError>,
) : AccessTokenSessionCommandRepository {

    val receivedUserIds = mutableListOf<UserId>()

    override suspend fun invalidateAllSessions(
        userId: UserId,
    ): ResultType<Unit, DataError> {
        receivedUserIds += userId

        return result
    }
}
