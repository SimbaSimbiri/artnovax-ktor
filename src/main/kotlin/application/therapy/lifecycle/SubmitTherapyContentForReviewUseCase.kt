package com.simbiri.application.lifecycle.therapy

import com.simbiri.application.therapy.context.TherapyContentContextLoader
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.therapy.TherapyContentStatus
import com.simbiri.domain.policy.therapy.TherapyContentAccessPolicy
import com.simbiri.domain.policy.therapy.TherapyContentLifecyclePolicy
import com.simbiri.domain.policy.therapy.TherapyContentPolicy
import com.simbiri.domain.repository.TherapyContentRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.time.Clock

class SubmitTherapyContentForReviewUseCase(
    private val therapyContentRepository: TherapyContentRepository,
    private val contextLoader: TherapyContentContextLoader,
    private val clock: Clock,
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
                operation = "submit therapy content for review",
            )?.let { error ->
                return ResultType.Failure(error)
            }

        TherapyContentPolicy.validateForReview(context.session)?.let { error ->
                return ResultType.Failure(error)
            }

        TherapyContentLifecyclePolicy.validateTransition(
                currentStatus = context.session.status,
                targetStatus = TherapyContentStatus.IN_REVIEW,
            )?.let { error ->
                return ResultType.Failure(error)
            }

        return therapyContentRepository.transitionStatus(
            therapySessionId = therapySessionId,
            expectedStatus = TherapyContentStatus.DRAFT,
            targetStatus = TherapyContentStatus.IN_REVIEW,
            transitionedAt = clock.instant(),
        )
    }
}
