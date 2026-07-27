package com.simbiri.application.therapy.session

import com.simbiri.application.therapy.context.TherapyContentContextLoader
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.policy.therapy.TherapyContentAccessPolicy
import com.simbiri.domain.policy.therapy.TherapyContentLifecyclePolicy
import com.simbiri.domain.repository.TherapyContentRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class DeleteTherapyDraftUseCase(
    private val therapyContentRepository: TherapyContentRepository,
    private val contextLoader: TherapyContentContextLoader,
) {

    suspend operator fun invoke(
        actorId: UserId,
        therapySessionId: TherapySessionId,
    ): ResultType<Unit, DataError> {
        val context = when (val result = contextLoader.load(
            actorId = actorId,
            therapySessionId = therapySessionId,
        )) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> return ResultType.Failure(result.error)
        }

        TherapyContentAccessPolicy.validateCanManageDraft(
                actor = context.actor,
                session = context.session,
                operation = "delete therapy draft",
            )?.let { error ->
                return ResultType.Failure(error)
            }

        TherapyContentLifecyclePolicy.validateDeletionAllowed(context.session)?.let { error ->
                return ResultType.Failure(error)
            }

        return therapyContentRepository.deleteDraft(therapySessionId)
    }
}
