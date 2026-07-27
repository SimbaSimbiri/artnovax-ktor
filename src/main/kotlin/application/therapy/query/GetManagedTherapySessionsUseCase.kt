package com.simbiri.application.therapy.query

import com.simbiri.application.therapy.context.TherapyContentContextLoader
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.therapy.TherapySession
import com.simbiri.domain.policy.therapy.TherapyContentAccessPolicy
import com.simbiri.domain.repository.TherapyContentRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Retrieves therapy content for authoring, review, and publication
 * workflows.
 *
 * Authors are automatically restricted to their own content. Reviewers
 * and publishers may optionally filter by another author.
 */
class GetManagedTherapySessionsUseCase(
    private val therapyContentRepository: TherapyContentRepository,
    private val contextLoader: TherapyContentContextLoader,
) {

    suspend operator fun invoke(
        actorId: UserId,
        filters: ManagedTherapyContentFilters =
            ManagedTherapyContentFilters(),
    ): ResultType<List<TherapySession>, DataError> {
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

        TherapyContentAccessPolicy
            .validateCanBrowseManagedContent(
                actor = actor,
                requestedAuthorId = filters.authorId,
            )
            ?.let { error ->
                return ResultType.Failure(error)
            }

        val persistedActorId =
            actor.id
                ?: return ResultType.Failure(
                    DataError.Forbidden(
                        message = "Therapy-content query failed. " +
                                "A persisted actor ID is required."
                    )
                )

        /*
         * Authors cannot broaden the repository query beyond their own
         * content. Reviewers and publishers retain the requested author
         * filter, including null for all authors.
         */
        val effectiveAuthorId =
            if (
                actor.canReviewTherapyContent ||
                actor.canPublishTherapyContent
            ) {
                filters.authorId
            } else {
                persistedActorId
            }

        return therapyContentRepository
            .getTherapySessions(
                status = filters.status,
                authorId = effectiveAuthorId,
                goal = filters.goal,
                intensity = filters.intensity,
                locale = filters.locale,
            )
    }
}
