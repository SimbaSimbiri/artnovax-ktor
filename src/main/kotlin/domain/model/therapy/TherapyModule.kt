package com.simbiri.domain.model.therapy

import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.common.Timestamp

/**
 * One ordered, executable step within a TherapySession.
 *
 * The parent session owns the module. The parent TherapySessionId is
 * therefore represented by the aggregate relationship rather than
 * duplicated inside this domain child.
 *
 * Persistence entities may still contain a therapy_session_id foreign
 * key.
 */
data class TherapyModule(
    val id: TherapyModuleId? = null,

    /**
     * Zero-based position inside the parent session.
     */
    val orderIndex: Int,

    val title: String,

    /**
     * A brief description of what the user should gain from this step.
     */
    val goal: String,

    /**
     * The guidance shown or narrated to the user.
     */
    val instructions: String,

    /**
     * Plain-language explanation of the therapeutic purpose.
     */
    val whyThisHelps: String,

    /**
     * Determines which client-side interaction engine should render the
     * module.
     */
    val modality: TherapyModality,

    val estimatedDurationSeconds: Int,

    val isSkippable: Boolean = false,
    val isRepeatable: Boolean = true,

    val assets: List<TherapyAsset> = emptyList(),

    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)
