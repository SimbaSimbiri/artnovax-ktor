package com.simbiri.application.therapy.query

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.therapy.TherapyContentStatus
import com.simbiri.domain.model.therapy.TherapyGoal
import com.simbiri.domain.model.therapy.TherapyIntensity

/**
 * Typed filters accepted by the therapy-content management query.
 *
 * HTTP strings and query-parameter parsing belong to presentation.
 */
data class ManagedTherapyContentFilters(
    val status: TherapyContentStatus? = null,
    val authorId: UserId? = null,
    val goal: TherapyGoal? = null,
    val intensity: TherapyIntensity? = null,
    val locale: String? = null,
)
