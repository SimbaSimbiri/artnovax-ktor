package com.simbiri.domain.policy.therapy

import com.simbiri.application.therapy.asset.TherapyAssetUploadRequest
import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.therapy.TherapyAssetRole
import com.simbiri.domain.model.therapy.TherapyMediaType
import com.simbiri.domain.util.DataError
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertIs
import kotlin.test.assertNull

class TherapyAssetUploadPolicyTest {

    @Test
    fun `valid session cover upload is accepted`() {
        val request = requestFixture(
            therapyModuleId = null,
            role = TherapyAssetRole.SESSION_COVER,
            mediaType = TherapyMediaType.IMAGE,
            mimeType = "image/png",
        )

        assertNull(TherapyAssetUploadPolicy.validate(request))
    }

    @Test
    fun `session cover cannot belong to a module`() {
        val request = requestFixture(
            role = TherapyAssetRole.SESSION_COVER,
            mediaType = TherapyMediaType.IMAGE,
            mimeType = "image/png",
        )

        val error = assertIs<DataError.ValidationError>(TherapyAssetUploadPolicy.validate(request))

        assertContains(error.message, "field=therapyModuleId")
    }

    @Test
    fun `invalid sha256 is rejected`() {
        val request = requestFixture(
            sha256 = "not-a-sha256",
        )

        val error = assertIs<DataError.ValidationError>(TherapyAssetUploadPolicy.validate(request))

        assertContains(error.message, "field=sha256")
    }

    @Test
    fun `media type and mime type must agree`() {
        val request = requestFixture(
            mediaType = TherapyMediaType.IMAGE,
            mimeType = "video/mp4",
        )

        val error = assertIs<DataError.ValidationError>(TherapyAssetUploadPolicy.validate(request))

        assertContains(error.message, "field=mimeType")
    }

    private fun requestFixture(
        therapyModuleId: TherapyModuleId? = TherapyModuleId(UUID.randomUUID()),
        role: TherapyAssetRole = TherapyAssetRole.PRIMARY_MEDIA,
        mediaType: TherapyMediaType = TherapyMediaType.IMAGE,
        mimeType: String = "image/png",
        sha256: String = "a".repeat(64),
    ): TherapyAssetUploadRequest = TherapyAssetUploadRequest(
        therapySessionId = TherapySessionId(UUID.randomUUID()),
        therapyModuleId = therapyModuleId,
        role = role,
        mediaType = mediaType,
        mimeType = mimeType,
        sizeBytes = 1_024L,
        sha256 = sha256,
        altText = "A calming blue pattern.",
    )
}
