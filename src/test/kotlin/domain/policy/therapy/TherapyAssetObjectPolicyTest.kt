package com.simbiri.domain.policy.therapy

import com.simbiri.application.therapy.asset.ConfirmTherapyAssetUploadRequest
import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.therapy.TherapyAssetRole
import com.simbiri.domain.model.therapy.TherapyMediaType
import com.simbiri.domain.storage.TherapyStoredObject
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TherapyAssetObjectPolicyTest {

    @Test
    fun `matching stored object is accepted`() {
        assertNull(
            TherapyAssetObjectPolicy.validate(
                request = REQUEST,
                storedObject = STORED_OBJECT,
            )
        )
    }

    @Test
    fun `checksum mismatch is rejected`() {
        assertNotNull(
            TherapyAssetObjectPolicy.validate(
                request = REQUEST,
                storedObject = STORED_OBJECT.copy(
                    sha256 = "b".repeat(64),
                ),
            )
        )
    }

    @Test
    fun `size mismatch is rejected`() {
        assertNotNull(
            TherapyAssetObjectPolicy.validate(
                request = REQUEST,
                storedObject = STORED_OBJECT.copy(
                    sizeBytes = 2_048L,
                ),
            )
        )
    }

    private companion object {
        val SESSION_ID = TherapySessionId(UUID.randomUUID())
        val MODULE_ID = TherapyModuleId(UUID.randomUUID())
        val STORAGE_KEY = "therapy-content/${SESSION_ID.value}/modules/${MODULE_ID.value}/${UUID.randomUUID()}"

        val REQUEST = ConfirmTherapyAssetUploadRequest(
            therapySessionId = SESSION_ID,
            therapyModuleId = MODULE_ID,
            role = TherapyAssetRole.PRIMARY_MEDIA,
            mediaType = TherapyMediaType.IMAGE,
            storageKey = STORAGE_KEY,
            mimeType = "image/png",
            sizeBytes = 1_024L,
            sha256 = "a".repeat(64),
            altText = "A calming pattern.",
        )

        val STORED_OBJECT = TherapyStoredObject(
            storageKey = STORAGE_KEY,
            mimeType = "image/png",
            sizeBytes = 1_024L,
            sha256 = "a".repeat(64),
        )
    }
}
