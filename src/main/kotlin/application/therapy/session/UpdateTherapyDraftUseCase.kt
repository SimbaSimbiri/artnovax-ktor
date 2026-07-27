package com.simbiri.application.therapy.session

import com.simbiri.application.therapy.context.TherapyContentContextLoader
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.therapy.TherapySession
import com.simbiri.domain.policy.therapy.TherapyContentAccessPolicy
import com.simbiri.domain.policy.therapy.TherapyContentLifecyclePolicy
import com.simbiri.domain.policy.therapy.TherapyContentPolicy
import com.simbiri.domain.repository.TherapyContentRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class UpdateTherapyDraftUseCase(
    private val therapyContentRepository: TherapyContentRepository,
    private val contextLoader: TherapyContentContextLoader,
) {

    suspend operator fun invoke(
        actorId: UserId,
        session: TherapySession,
    ): ResultType<Unit, DataError> {
        val therapySessionId = session.id ?: return ResultType.Failure(
            DataError.ValidationError(
                message = "Therapy draft update failed. A persisted therapy-session ID is required."
            )
        )

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
                operation = "update therapy draft details",
            )?.let { error ->
                return ResultType.Failure(error)
            }

        TherapyContentLifecyclePolicy.validateContentMutationAllowed(
                context.session
            )?.let { error ->
                return ResultType.Failure(error)
            }

        /*
         * updateDraftDetails does not alter modules or lifecycle state.
         */
        val candidate = session.copy(
            status = context.session.status,
            modules = context.session.modules,
            createdAt = context.session.createdAt,
            updatedAt = context.session.updatedAt,
            publishedAt = context.session.publishedAt,
            archivedAt = context.session.archivedAt,
        )

        TherapyContentPolicy.validateDraft(candidate)?.let { error ->
                return ResultType.Failure(error)
            }

        return therapyContentRepository.updateDraftDetails(candidate)
    }
}
