package com.simbiri.domain.policy.therapy

import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.util.DataError
import java.util.UUID

object TherapyAssetStorageKeyPolicy {

    fun validate(
        therapySessionId: TherapySessionId,
        therapyModuleId: TherapyModuleId?,
        storageKey: String,
    ): DataError.ValidationError? {
        val normalizedKey = storageKey.trim()

        val expectedPrefix = if (therapyModuleId == null) {
            "therapy-content/${therapySessionId.value}/session/"
        } else {
            "therapy-content/${therapySessionId.value}/modules/${therapyModuleId.value}/"
        }

        if (!normalizedKey.startsWith(expectedPrefix)) {
            return DataError.ValidationError(
                message = "Therapy asset storage key is invalid. The key does not belong to the requested session " +
                        "and module scope."
            )
        }

        val objectIdentifier = normalizedKey.removePrefix(expectedPrefix)

        if (objectIdentifier.contains("/") || objectIdentifier.toUuidOrNull() == null) {
            return DataError.ValidationError(
                message = "Therapy asset storage key is invalid. The object identifier must be a UUID."
            )
        }

        return null
    }

    private fun String.toUuidOrNull(): UUID? =
        try {
            UUID.fromString(this)
        } catch (_: IllegalArgumentException) {
            null
        }
}
