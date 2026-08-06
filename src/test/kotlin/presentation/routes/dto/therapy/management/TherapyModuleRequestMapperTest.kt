package com.simbiri.presentation.routes.dto.therapy.management

import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.therapy.TherapyModality
import com.simbiri.domain.model.therapy.TherapyModule
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TherapyModuleRequestMapperTest {

    @Test
    fun `create request maps normalized module metadata`() {
        val request = CreateTherapyModuleRequestDto(
            orderIndex = 1,
            title = "  Grounding Through Color  ",
            goal = "  Reduce tension through slow movement.  ",
            instructions = "  Draw one line for each breath.  ",
            whyThisHelps = "  Slow repeated movement can support focused attention.  ",
            modality = "guided-audio",
            estimatedDurationSeconds = 180,
            isSkippable = true,
            isRepeatable = false,
        )

        val result = request.toTherapyModuleOrFailure()
        val success = assertIs<ResultType.Success<TherapyModule>>(result)
        val module = success.data

        assertNull(module.id)
        assertEquals(1, module.orderIndex)
        assertEquals("Grounding Through Color", module.title)
        assertEquals("Reduce tension through slow movement.", module.goal)
        assertEquals("Draw one line for each breath.", module.instructions)
        assertEquals("Slow repeated movement can support focused attention.", module.whyThisHelps)
        assertEquals(TherapyModality.GUIDED_AUDIO, module.modality)
        assertEquals(180, module.estimatedDurationSeconds)
        assertTrue(module.isSkippable)
        assertEquals(false, module.isRepeatable)
        assertTrue(module.assets.isEmpty())
    }

    @Test
    fun `update request receives module id from route`() {
        val therapyModuleId = TherapyModuleId(UUID.randomUUID())

        val request = UpdateTherapyModuleRequestDto(
            title = "Reflection",
            goal = "Notice emotional changes.",
            instructions = "Write one sentence about how you feel.",
            whyThisHelps = "Reflection can make emotional changes easier to recognize.",
            modality = "reflection",
            estimatedDurationSeconds = 120,
        )

        val result = request.toTherapyModuleOrFailure(therapyModuleId)
        val success = assertIs<ResultType.Success<TherapyModule>>(result)

        assertEquals(therapyModuleId, success.data.id)
        assertEquals(TherapyModality.REFLECTION, success.data.modality)
        assertTrue(success.data.assets.isEmpty())
    }

    @Test
    fun `unsupported modality returns validation failure`() {
        val request = CreateTherapyModuleRequestDto(
            orderIndex = 0,
            title = "Grounding",
            goal = "",
            instructions = "",
            whyThisHelps = "",
            modality = "virtual-reality",
            estimatedDurationSeconds = 60,
        )

        val result = request.toTherapyModuleOrFailure()
        val failure = assertIs<ResultType.Failure<DataError>>(result)
        val validationError = assertIs<DataError.ValidationError>(failure.error)

        assertContains(validationError.message, "field=modality")
        assertContains(validationError.message, "Allowed values")
    }

    @Test
    fun `reorder request parses module ids in supplied order`() {
        val firstId = UUID.randomUUID()
        val secondId = UUID.randomUUID()

        val request = ReorderTherapyModulesRequestDto(
            orderedModuleIds = listOf(
                firstId.toString(),
                secondId.toString(),
            )
        )

        val result = request.toTherapyModuleIdsOrFailure()
        val success = assertIs<ResultType.Success<List<TherapyModuleId>>>(result)

        assertEquals(
            listOf(
                TherapyModuleId(firstId),
                TherapyModuleId(secondId),
            ),
            success.data,
        )
    }

    @Test
    fun `reorder request rejects invalid module id`() {
        val request = ReorderTherapyModulesRequestDto(
            orderedModuleIds = listOf("not-a-uuid")
        )

        val result = request.toTherapyModuleIdsOrFailure()
        val failure = assertIs<ResultType.Failure<DataError>>(result)
        val validationError = assertIs<DataError.ValidationError>(failure.error)

        assertContains(validationError.message, "orderedModuleIds[0]")
        assertContains(validationError.message, "valid UUID")
    }
}
