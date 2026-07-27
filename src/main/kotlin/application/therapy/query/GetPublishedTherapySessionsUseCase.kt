package com.simbiri.application.therapy.query

import com.simbiri.domain.model.therapy.TherapyContentStatus
import com.simbiri.domain.model.therapy.TherapyGoal
import com.simbiri.domain.model.therapy.TherapyIntensity
import com.simbiri.domain.model.therapy.TherapySession
import com.simbiri.domain.repository.TherapyContentRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Retrieves therapy sessions visible through the public application
 * catalogue.
 *
 * Status is fixed to PUBLISHED so clients cannot use public query
 * parameters to access drafts, reviewed content, or archived versions.
 */
class GetPublishedTherapySessionsUseCase(
    private val therapyContentRepository: TherapyContentRepository,
) {

    suspend operator fun invoke(
        goal: TherapyGoal? = null,
        intensity: TherapyIntensity? = null,
        locale: String? = null,
    ): ResultType<List<TherapySession>, DataError> =
        therapyContentRepository.getTherapySessions(
            status = TherapyContentStatus.PUBLISHED,
            authorId = null,
            goal = goal,
            intensity = intensity,
            locale = locale,
        )
}
