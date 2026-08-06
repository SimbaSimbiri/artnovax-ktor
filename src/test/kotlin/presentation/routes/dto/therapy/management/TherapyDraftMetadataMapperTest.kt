package com.simbiri.presentation.routes.dto.therapy.management

import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.therapy.TherapeuticPriority
import com.simbiri.domain.model.therapy.TherapyContentStatus
import com.simbiri.domain.model.therapy.TherapyContraindication
import com.simbiri.domain.model.therapy.TherapyGoal
import com.simbiri.domain.model.therapy.TherapyIntensity
import com.simbiri.domain.model.therapy.TherapySession
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.util.UUID
import kotlin.enums.enumEntries
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TherapyDraftMetadataMapperTest {

    @Test
    fun `valid request creates server-owned draft`() {
        val actorId = UserId(
            UUID.randomUUID()
        )

        val goal = enumEntries<TherapyGoal>().first()

        val contraindication = enumEntries<TherapyContraindication>().first()

        val request = TherapyDraftMetadataRequestDto(
            title = "  Grounding Through Color  ",
            description = "  Breathe, draw, and reflect.  ",
            tagline = "  Let each stroke be a breath  ",
            therapeuticPriority = "mental-health",
            intensity = "gentle",
            locale = "en_us",
            goalTags = listOf(
                goal.name.lowercase().replace(
                        oldChar = '_',
                        newChar = '-',
                    )
            ),

            contraindications = listOf(
                contraindication.name
            ),

            cultureTags = listOf(
                "  Mongolian  ",
                "African Patterns",
            ),
        )

        val result = request.toNewTherapyDraftOrFailure(
                authenticatedUserId = actorId
            )

        val success = assertIs<ResultType.Success<TherapySession>>(result)

        val draft = success.data

        assertNull(
            draft.id
        )

        assertNull(
            draft.seriesId
        )

        assertEquals(
            expected = actorId,
            actual = draft.authorId,
        )

        assertEquals(
            expected = "Grounding Through Color",
            actual = draft.title,
        )

        assertEquals(
            expected = "Breathe, draw, and reflect.",
            actual = draft.description,
        )

        assertEquals(
            expected = "Let each stroke be a breath",
            actual = draft.tagline,
        )

        assertEquals(
            expected = TherapyContentStatus.DRAFT,
            actual = draft.status,
        )

        assertEquals(
            expected = 1,
            actual = draft.version,
        )

        assertEquals(
            expected = TherapeuticPriority.MENTAL_HEALTH,
            actual = draft.therapeuticPriority,
        )

        assertEquals(
            expected = TherapyIntensity.GENTLE,
            actual = draft.intensity,
        )

        assertEquals(
            expected = "en-US",
            actual = draft.locale,
        )

        assertEquals(
            expected = setOf(goal),
            actual = draft.goalTags,
        )

        assertEquals(
            expected = setOf(
                contraindication
            ),
            actual = draft.contraindications,
        )

        assertEquals(
            expected = setOf(
                "Mongolian",
                "African Patterns",
            ),
            actual = draft.cultureTags,
        )

        assertTrue(
            draft.modules.isEmpty()
        )

        assertNull(
            draft.coverAsset
        )
    }

    @Test
    fun `update candidate receives path session id`() {
        val actorId = UserId(
            UUID.randomUUID()
        )

        val therapySessionId = TherapySessionId(
            UUID.randomUUID()
        )

        val request = TherapyDraftMetadataRequestDto(
            title = "Poetic Doodles",
            description = "A reflective poetry and doodling session.",
            intensity = "moderate",
            locale = "sw-KE",
        )

        val result = request.toTherapyDraftUpdateOrFailure(
                authenticatedUserId = actorId,

                therapySessionId = therapySessionId,
            )

        val success = assertIs<ResultType.Success<TherapySession>>(result)

        assertEquals(
            expected = therapySessionId,
            actual = success.data.id,
        )

        assertEquals(
            expected = actorId,
            actual = success.data.authorId,
        )
    }

    @Test
    fun `unsupported intensity returns validation failure`() {
        val request = TherapyDraftMetadataRequestDto(
            title = "Grounding",
            description = "Grounding exercise.",
            intensity = "extreme",
            locale = "en",
        )

        val result = request.toNewTherapyDraftOrFailure(
                authenticatedUserId = UserId(
                    UUID.randomUUID()
                )
            )

        val failure = assertIs<ResultType.Failure<DataError>>(result)

        val validationError = assertIs<DataError.ValidationError>(failure.error)

        assertContains(
            charSequence = validationError.message,
            other = "field=intensity",
        )
    }

    @Test
    fun `duplicate culture tags are rejected case insensitively`() {
        val request = TherapyDraftMetadataRequestDto(
            title = "Ubuntu Flow",
            description = "Pattern-based grounding.",
            intensity = "gentle",
            locale = "en",
            cultureTags = listOf(
                "Ubuntu",
                "  ubuntu  ",
            ),
        )

        val result = request.toNewTherapyDraftOrFailure(
                authenticatedUserId = UserId(
                    UUID.randomUUID()
                )
            )

        val failure = assertIs<ResultType.Failure<DataError>>(result)

        val validationError = assertIs<DataError.ValidationError>(failure.error)

        assertContains(
            charSequence = validationError.message,
            other = "Duplicate culture tag",
        )
    }

    @Test
    fun `duplicate enum values are rejected after normalization`() {
        val goal = enumValues<TherapyGoal>().first()

        val request = TherapyDraftMetadataRequestDto(
            title = "Calm Lines",
            description = "A gentle line exercise.",
            intensity = "gentle",
            locale = "en",
            goalTags = listOf(
                goal.name,
                goal.name.lowercase().replace(
                        oldChar = '_',
                        newChar = '-',
                    ),
            ),
        )

        val result = request.toNewTherapyDraftOrFailure(
                authenticatedUserId = UserId(
                    UUID.randomUUID()
                )
            )

        val failure = assertIs<ResultType.Failure<DataError>>(result)

        val validationError = assertIs<DataError.ValidationError>(failure.error)

        assertContains(
            charSequence = validationError.message,
            other = "Duplicate value",
        )
    }
}
