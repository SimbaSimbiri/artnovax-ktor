package com.simbiri.application.therapy.asset

import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.common.TherapySessionId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class TherapyAssetStorageKeyFactoryTest {

    @Test
    fun `session asset key contains session scope`() {
        val factory = TherapyAssetStorageKeyFactory(
            uuidProvider = { OBJECT_ID }
        )

        val key = factory.create(
            therapySessionId = SESSION_ID,
            therapyModuleId = null,
        )

        assertEquals(
            "therapy-content/${SESSION_ID.value}/session/$OBJECT_ID",
            key,
        )
    }

    @Test
    fun `module asset key contains session and module scope`() {
        val factory = TherapyAssetStorageKeyFactory(
            uuidProvider = { OBJECT_ID }
        )

        val key = factory.create(
            therapySessionId = SESSION_ID,
            therapyModuleId = MODULE_ID,
        )

        assertEquals(
            "therapy-content/${SESSION_ID.value}/modules/${MODULE_ID.value}/$OBJECT_ID",
            key,
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
