package com.simbiri.application.therapy.asset

import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.common.TherapySessionId
import java.util.UUID

class TherapyAssetStorageKeyFactory(
    private val uuidProvider: () -> UUID = UUID::randomUUID,
) {

    fun create(
        therapySessionId: TherapySessionId,
        therapyModuleId: TherapyModuleId?,
    ): String {
        val objectId = uuidProvider()

        return if (therapyModuleId == null) {
            "therapy-content/${therapySessionId.value}/session/$objectId"
        } else {
            "therapy-content/${therapySessionId.value}/modules/${therapyModuleId.value}/$objectId"
        }
    }
}
