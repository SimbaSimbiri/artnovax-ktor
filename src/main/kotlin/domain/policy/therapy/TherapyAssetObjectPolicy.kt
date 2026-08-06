package com.simbiri.domain.policy.therapy

import com.simbiri.application.therapy.asset.ConfirmTherapyAssetUploadRequest
import com.simbiri.domain.storage.TherapyStoredObject
import com.simbiri.domain.util.DataError

object TherapyAssetObjectPolicy {

    fun validate(
        request: ConfirmTherapyAssetUploadRequest,
        storedObject: TherapyStoredObject,
    ): DataError.Conflict? = when {
        storedObject.storageKey != request.storageKey.trim() -> {
            conflict(
                reason = "The inspected object uses a different storage key."
            )
        }

        storedObject.sizeBytes != request.sizeBytes -> {
            conflict(
                reason = "Uploaded size ${storedObject.sizeBytes} does not match expected size ${request.sizeBytes}."
            )
        }

        storedObject.mimeType == null ||
                !storedObject.mimeType.equals(request.mimeType.trim(), ignoreCase = true) -> {
            conflict(
                reason = "Uploaded MIME type '${storedObject.mimeType}' does not match '${request.mimeType}'."
            )
        }

        storedObject.sha256 == null ||
                !storedObject.sha256.equals(request.sha256.trim(), ignoreCase = true) -> {
            conflict(
                reason = "The uploaded object failed SHA-256 verification."
            )
        }

        else -> null
    }

    private fun conflict(reason: String): DataError.Conflict =
        DataError.Conflict(
            message = "Therapy asset confirmation failed. $reason"
        )
}
