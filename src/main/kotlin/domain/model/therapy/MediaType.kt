package com.simbiri.domain.model.therapy

enum class MediaType {
    TEXT, // for modules like POETRY
    CANVAS,
    AUDIO,
    VIDEO, // if module type is OTHER, and a psychologist speaks directly to the patient through an exercise
    TEXT_AND_AUDIO, // for modules like AFFIRMATIONS
    TEXT_AND_VIDEO, // for modules like BREATHING where a slider is shown
    MIXED
}