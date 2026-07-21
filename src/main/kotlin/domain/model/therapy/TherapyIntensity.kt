package com.simbiri.domain.model.therapy

/**
 * Approximate emotional intensity of a therapy session.
 *
 * This now replaces storing a PHQ value on the content itself. Screening and
 * recommendation logic will use this to select appropriate
 * content.
 */
enum class TherapyIntensity {
    GENTLE,
    MODERATE,
    INTENSE,
}