package com.simbiri.application.therapy.asset

import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.therapy.TherapyAsset
import com.simbiri.domain.model.therapy.TherapyAssetRole
import com.simbiri.domain.model.therapy.TherapyMediaType

data class ConfirmTherapyAssetUploadRequest(
    val therapySessionId: TherapySessionId,
    val therapyModuleId: TherapyModuleId?,
    val role: TherapyAssetRole,
    val mediaType: TherapyMediaType,
    val storageKey: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
    val locale: String? = null,
    val altText: String? = null,
    val transcript: String? = null,
) {

    fun toUploadValidationRequest(): TherapyAssetUploadRequest =
        TherapyAssetUploadRequest(
            therapySessionId = therapySessionId,
            therapyModuleId = therapyModuleId,
            role = role,
            mediaType = mediaType,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            sha256 = sha256,
            locale = locale,
            altText = altText,
            transcript = transcript,
        )

    fun toTherapyAsset(): TherapyAsset =
        TherapyAsset(
            role = role,
            mediaType = mediaType,
            storageKey = storageKey,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            sha256 = sha256,
            locale = locale,
            altText = altText,
            transcript = transcript,
        )
}
