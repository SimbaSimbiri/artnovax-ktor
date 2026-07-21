package com.simbiri.domain.model.therapy

/**
 * Identifies the primary purpose of authored content.
 *
 * Recommendation logic prioritizes MENTAL_HEALTH content over
 * ART_SKILL content when both are appropriate.
 */
enum class TherapeuticPriority {
    MENTAL_HEALTH,
    ART_SKILL,
}