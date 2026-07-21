package com.simbiri.domain.model.therapy

/**
 * Safety characteristics that may make a session inappropriate for some
 * users or circumstances.
 *
 * These values describe the content. The recommendation and safety
 * layers will later compare them against user screening and preferences.
 */
enum class TherapyContraindication {
    HIGH_EMOTIONAL_INTENSITY,
    TRAUMA_RELATED_CONTENT,
    GRIEF_RELATED_CONTENT,
    BREATH_HOLDING,
    FLASHING_VISUALS,
    LOUD_AUDIO,
    PHYSICAL_MOVEMENT,
    OTHER,
}