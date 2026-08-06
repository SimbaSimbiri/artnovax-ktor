package com.simbiri.presentation.routes.dto.therapy.management

import kotlinx.serialization.Serializable

/**
 * Complete ordered list of every module currently owned by the therapy session.
 */
@Serializable
data class ReorderTherapyModulesRequestDto(
    val orderedModuleIds: List<String>,
)
