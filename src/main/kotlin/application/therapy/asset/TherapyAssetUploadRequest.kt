package com.simbiri.application.therapy.asset

import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.therapy.TherapyAssetRole
import com.simbiri.domain.model.therapy.TherapyMediaType

data class TherapyAssetUploadRequest(
    val therapySessionId: TherapySessionId,
    val therapyModuleId: TherapyModuleId?,
    val role: TherapyAssetRole,
    val mediaType: TherapyMediaType,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
    val locale: String? = null,
    val altText: String? = null,
    val transcript: String? = null,
)
