package com.simbiri.application.therapy.query

import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.therapy.TherapyContentStatus
import com.simbiri.domain.model.therapy.TherapySession
import com.simbiri.domain.repository.TherapyContentRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Retrieves one publicly available therapy session.
 *
 * Non-published content is returned as NotFound rather than Forbidden so
 * public API does not disclose existence of  a private draft or reviewed
 * session
 */
class GetPublishedTherapySessionByIdUseCase(
    private val therapyContentRepository: TherapyContentRepository,
) {

    suspend operator fun invoke(
        therapySessionId: TherapySessionId,
    ): ResultType<TherapySession, DataError> =
        when (
            val result =
                therapyContentRepository
                    .getTherapySessionById(
                        therapySessionId
                    )
        ) {
            is ResultType.Success -> {
                if (
                    result.data.status ==
                    TherapyContentStatus.PUBLISHED
                ) {
                    result
                } else {
                    ResultType.Failure(
                        DataError.NotFound
                    )
                }
            }

            is ResultType.Failure ->
                ResultType.Failure(result.error)
        }
}