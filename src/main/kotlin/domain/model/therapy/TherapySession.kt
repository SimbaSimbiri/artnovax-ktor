package com.simbiri.domain.model.therapy

import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.TherapySessionSeriesId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId

/**
 * Aggregate root for reusable, authored therapy content.
 *
 * A TherapySession owns its ordered TherapyModules and session-level
 * assets. User progress, ratings, emotion snapshots, and completion
 * records belong to the separate Therapy Execution aggregate.
 */
data class TherapySession(
    val id: TherapySessionId? = null,
    /**
     * Shared by every version of the same authored therapy session.
     *
     * It is null for a new, unpersisted first version. Persistence assigns
     * it when the initial draft is created.
     */
    val seriesId: TherapySessionSeriesId? = null,
    val authorId: UserId,

    val title: String,
    val description: String,
    val intensity: TherapyIntensity,
    val locale: String,

    val tagline: String? = null,

    val status: TherapyContentStatus =
        TherapyContentStatus.DRAFT,

    val version: Int = 1,

    val therapeuticPriority: TherapeuticPriority =
        TherapeuticPriority.MENTAL_HEALTH,

    val goalTags: Set<TherapyGoal> = emptySet(),

    val contraindications: Set<TherapyContraindication> =
        emptySet(),

    val cultureTags: Set<String> = emptySet(),

    val coverAsset: TherapyAsset? = null,

    val modules: List<TherapyModule> = emptyList(),

    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val publishedAt: Timestamp? = null,
    val archivedAt: Timestamp? = null,
) {
    /**
     * Derived from the modules owned by this aggregate.
     *
     * This value must not be accepted from an HTTP request or stored as
     * independently writable domain state.
     */
    val moduleCount: Int
        get() = modules.size

    val estimatedDurationSeconds: Int
        get() = modules.sumOf { module ->
            module.estimatedDurationSeconds
        }

    /**
     * Rounds partial minutes upward so a 61-second session is displayed
     * as approximately two minutes rather than one.
     */
    val estimatedDurationMinutes: Int
        get() {
            val totalSeconds = estimatedDurationSeconds

            return if (totalSeconds == 0) {
                0
            } else {
                (totalSeconds + 59) / 60
            }
        }
}
