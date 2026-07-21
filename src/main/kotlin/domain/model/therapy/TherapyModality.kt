package com.simbiri.domain.model.therapy

/**
 * Primary interaction performed by the user during a therapy module.
 *
 * Media such as audio, video, and images are now represented separately as
 * TherapyAssets.
 */
enum class TherapyModality {
    BREATHING,
    PROMPT,
    DRAWING,
    DOODLING,
    PAINTING,
    REFLECTION,
    JOURNALING,
    POETRY,
    AFFIRMATION,
    GUIDED_AUDIO,
    GUIDED_VIDEO,
    OTHER,
}