package com.simbiri.application.therapy.query

import com.simbiri.application.therapy.context.TherapyContentContextLoader
import com.simbiri.domain.model.common.TherapySessionSeriesId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.therapy.TherapySession
import com.simbiri.domain.policy.therapy.TherapyContentAccessPolicy
import com.simbiri.domain.repository.TherapyContentRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Retrieves highest persisted version of a therapy-session series
 * for management workflow.
 *
 * This is strictly management-only. The repository's
 * latest version may be a draft or review version and must not be exposed
 * through the public catalogue.
 */
class GetLatestManagedTherapySessionVersionUseCase(
    private val therapyContentRepository: TherapyContentRepository,
    private val contextLoader: TherapyContentContextLoader,
) {

    suspend operator fun invoke(
        actorId: UserId,
        seriesId: TherapySessionSeriesId,
    ): ResultType<TherapySession, DataError> {
        val actor =
            when (
                val result =
                    contextLoader.loadActor(actorId)
            ) {
                is ResultType.Success ->
                    result.data

                is ResultType.Failure ->
                    return ResultType.Failure(
                        result.error
                    )
            }

        val session =
            when (
                val result =
                    therapyContentRepository
                        .getLatestTherapySessionVersion(
                            seriesId
                        )
            ) {
                is ResultType.Success ->
                    result.data

                is ResultType.Failure ->
                    return ResultType.Failure(
                        result.error
                    )
            }

        TherapyContentAccessPolicy
            .validateCanViewManagedContent(
                actor = actor,
                session = session,
            )
            ?.let { error ->
                return ResultType.Failure(error)
            }

        return ResultType.Success(session)
    }
}
