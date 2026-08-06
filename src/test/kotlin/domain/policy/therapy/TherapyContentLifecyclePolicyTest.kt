package com.simbiri.domain.policy.therapy

import com.simbiri.domain.model.therapy.TherapyContentStatus
import com.simbiri.domain.util.DataError
import kotlin.enums.enumEntries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class TherapyContentLifecyclePolicyTest {

    @Test
    fun `supported lifecycle transitions are allowed`() {
        val supportedTransitions = setOf(
            TherapyContentStatus.DRAFT to TherapyContentStatus.IN_REVIEW,
            TherapyContentStatus.IN_REVIEW to TherapyContentStatus.DRAFT,
            TherapyContentStatus.IN_REVIEW to TherapyContentStatus.PUBLISHED,
            TherapyContentStatus.PUBLISHED to TherapyContentStatus.ARCHIVED,
        )

        supportedTransitions.forEach { (currentStatus, targetStatus) ->
            assertNull(
                TherapyContentLifecyclePolicy.validateTransition(
                    currentStatus = currentStatus,
                    targetStatus = targetStatus,
                )
            )
        }
    }

    @Test
    fun `all unsupported lifecycle transitions are rejected`() {
        val supportedTransitions = setOf(
            TherapyContentStatus.DRAFT to TherapyContentStatus.IN_REVIEW,
            TherapyContentStatus.IN_REVIEW to TherapyContentStatus.DRAFT,
            TherapyContentStatus.IN_REVIEW to TherapyContentStatus.PUBLISHED,
            TherapyContentStatus.PUBLISHED to TherapyContentStatus.ARCHIVED,
        )

        for (currentStatus in enumEntries<TherapyContentStatus>()) {
            for (targetStatus in enumEntries<TherapyContentStatus>()) {
                if (currentStatus to targetStatus in supportedTransitions) {
                    continue
                }

                val result = TherapyContentLifecyclePolicy.validateTransition(
                    currentStatus = currentStatus,
                    targetStatus = targetStatus,
                )

                assertIs<DataError.Conflict>(result)
            }
        }
    }

    @Test
    fun `allowed targets match the authored content workflow`() {
        assertEquals(
            expected = setOf(TherapyContentStatus.IN_REVIEW),
            actual = TherapyContentLifecyclePolicy.allowedTargets(TherapyContentStatus.DRAFT),
        )

        assertEquals(
            expected = setOf(
                TherapyContentStatus.DRAFT,
                TherapyContentStatus.PUBLISHED,
            ),
            actual = TherapyContentLifecyclePolicy.allowedTargets(TherapyContentStatus.IN_REVIEW),
        )

        assertEquals(
            expected = setOf(TherapyContentStatus.ARCHIVED),
            actual = TherapyContentLifecyclePolicy.allowedTargets(TherapyContentStatus.PUBLISHED),
        )

        assertEquals(
            expected = emptySet(),
            actual = TherapyContentLifecyclePolicy.allowedTargets(TherapyContentStatus.ARCHIVED),
        )
    }
}