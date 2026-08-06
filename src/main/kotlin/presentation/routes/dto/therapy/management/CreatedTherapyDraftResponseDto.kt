package com.simbiri.presentation.routes.dto.therapy.management

import kotlinx.serialization.Serializable

/**
 * Identifies a newly created therapy-session draft.
 */
@Serializable
data class CreatedTherapyDraftResponseDto(
    val therapySessionId: String,
    val status: String,
    val version: Int,
)
