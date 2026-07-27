package com.simbiri.application.therapy.session

import com.simbiri.application.therapy.context.TherapyContentContextLoader
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.therapy.TherapySession
import com.simbiri.domain.policy.therapy.TherapyContentAccessPolicy
import com.simbiri.domain.policy.therapy.TherapyContentPolicy
import com.simbiri.domain.repository.TherapyContentRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class CreateTherapyDraftUseCase(
    private val therapyContentRepository: TherapyContentRepository,
    private val contextLoader: TherapyContentContextLoader,
) {

    suspend operator fun invoke(
        actorId: UserId,
        session: TherapySession,
    ): ResultType<TherapySessionId, DataError> {
        if (session.id != null) {
            return ResultType.Failure(
                DataError.ValidationError(
                    message = "Therapy draft creation failed. A new draft must not already have an ID. "
                            + "receivedTherapySessionId=${session.id.value}."
                )
            )
        }

        val actor = when (val result = contextLoader.loadActor(actorId)) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> return ResultType.Failure(result.error)
        }

        TherapyContentAccessPolicy.validateCanCreateDraft(
                actor = actor,
                session = session,
            )?.let { error ->
                return ResultType.Failure(error)
            }

        TherapyContentPolicy.validateDraft(session)?.let { error ->
                return ResultType.Failure(error)
            }

        return therapyContentRepository.createDraft(session)
    }
}
