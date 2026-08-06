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

            is ResultType.Failure -> return ResultType.Failure(
                result.error
            )
        }

        TherapyContentAccessPolicy.validateCanManageDraft(
                actor = context.actor,

                session = context.session,

                operation = "update therapy draft details",
            )?.let { error ->
                return ResultType.Failure(
                    error
                )
            }

        TherapyContentLifecyclePolicy.validateContentMutationAllowed(
                context.session
            )?.let { error ->
                return ResultType.Failure(
                    error
                )
            }

        /*
         * Begins with the persisted aggregate and copy only fields that this
         * metadata operation permits the caller to edit.
         *
         */
        val candidate = context.session.copy(
            title = session.title,
            description = session.description,
            tagline = session.tagline,
            therapeuticPriority = session.therapeuticPriority,
            intensity = session.intensity,
            locale = session.locale,
            goalTags = session.goalTags,
            contraindications = session.contraindications,
            cultureTags = session.cultureTags,
        )

        TherapyContentPolicy.validateDraft(
                candidate
            )?.let { error ->
                return ResultType.Failure(
                    error
                )
            }

        return therapyContentRepository.updateDraftDetails(
                candidate
            )
    }
}
