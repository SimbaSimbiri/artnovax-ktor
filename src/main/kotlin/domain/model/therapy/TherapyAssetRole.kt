package com.simbiri.domain.model.therapy

/**
 * Describes how an asset is used within a session or module.
 */
enum class TherapyAssetRole {
    SESSION_COVER,
    PRIMARY_MEDIA,
    BACKGROUND_AUDIO,
    BACKGROUND_VIDEO,
    BACKGROUND_IMAGE,
    CAPTION,
    TRANSCRIPT,
}