package com.simbiri.domain.policy.therapy

import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.common.TherapySessionId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TherapyAssetStorageKeyPolicyTest {

    @Test
    fun `session key with matching scope is accepted`() {
        val storageKey = "therapy-content/${SESSION_ID.value}/session/$OBJECT_ID"

        assertNull(
            TherapyAssetStorageKeyPolicy.validate(
                therapySessionId = SESSION_ID,
                therapyModuleId = null,
                storageKey = storageKey,
            )
        )
    }

    @Test
    fun `module key with matching scope is accepted`() {
        val storageKey = "therapy-content/${SESSION_ID.value}/modules/${MODULE_ID.value}/$OBJECT_ID"

        assertNull(
            TherapyAssetStorageKeyPolicy.validate(
                therapySessionId = SESSION_ID,
                therapyModuleId = MODULE_ID,
                storageKey = storageKey,
            )
        )
    }

    @Test
    fun `key belonging to another module is rejected`() {
        val otherModuleId = UUID.randomUUID()
        val storageKey = "therapy-content/${SESSION_ID.value}/modules/$otherModuleId/$OBJECT_ID"

        assertNotNull(
            TherapyAssetStorageKeyPolicy.validate(
                therapySessionId = SESSION_ID,
                therapyModuleId = MODULE_ID,
                storageKey = storageKey,
            )
        )
    }

    private companion object {
        val SESSION_ID = TherapySessionId(
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        )
        val MODULE_ID = TherapyModuleId(
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        )
        val OBJECT_ID: UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
    }
}
