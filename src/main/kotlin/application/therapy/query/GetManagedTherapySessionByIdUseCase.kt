package com.simbiri.application.therapy.query

import com.simbiri.application.therapy.context.TherapyContentContextLoader
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.therapy.TherapySession
import com.simbiri.domain.policy.therapy.TherapyContentAccessPolicy
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Retrieves one therapy-content aggregate for an authoring, review, or
 * publication workflow.
 */
class GetManagedTherapySessionByIdUseCase(
    private val contextLoader: TherapyContentContextLoader,
) {

    suspend operator fun invoke(
        actorId: UserId,
        therapySessionId: TherapySessionId,
    ): ResultType<TherapySession, DataError> {
        val context =
            when (
                val result =
                    contextLoader.load(
                        actorId = actorId,
                        therapySessionId = therapySessionId,
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
                actor = context.actor,
                session = context.session,
            )
            ?.let { error ->
                return ResultType.Failure(error)
            }

        return ResultType.Success(
            context.session
        )
    }
}
