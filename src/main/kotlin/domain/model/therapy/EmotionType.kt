package com.simbiri.domain.model.therapy

enum class EmotionType {
    CALM,
    ANXIOUS, // negative emotions with higher pHQ rating will make system recommend for more calming sessions
    SAD,
    HAPPY,
    NEUTRAL,
    ANGRY,
    OTHER
}