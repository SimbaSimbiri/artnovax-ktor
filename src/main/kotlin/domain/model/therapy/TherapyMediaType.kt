package com.simbiri.domain.model.therapy

/**
 * Physical representation of a therapy asset.
 *
 * Canvas interaction is represented by TherapyModality and is not a
 * media type.
 */
enum class TherapyMediaType {
    IMAGE,
    AUDIO,
    VIDEO,
    TEXT,
}