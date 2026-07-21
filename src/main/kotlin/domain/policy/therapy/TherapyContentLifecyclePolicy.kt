package com.simbiri.domain.policy.therapy

import com.simbiri.domain.model.therapy.TherapyContentStatus
import com.simbiri.domain.model.therapy.TherapySession
import com.simbiri.domain.util.DataError

/**
 * Controls TherapySession lifecycle transitions and mutation rules.
 * answers whether an operation is allowed from the current state.
 */
object TherapyContentLifecyclePolicy {

    private val ALLOWED_TRANSITIONS:
            Map<TherapyContentStatus, Set<TherapyContentStatus>> =
        mapOf(
            TherapyContentStatus.DRAFT to
                    setOf(
                        TherapyContentStatus.IN_REVIEW,
                    ),

            TherapyContentStatus.IN_REVIEW to
                    setOf(
                        TherapyContentStatus.DRAFT,
                        TherapyContentStatus.PUBLISHED,
                    ),

            TherapyContentStatus.PUBLISHED to
                    setOf(
                        TherapyContentStatus.ARCHIVED,
                    ),

            TherapyContentStatus.ARCHIVED to
                    emptySet(),
        )

    /**
     * Valid lifecycle:
     *
     * DRAFT -> IN_REVIEW -> PUBLISHED -> ARCHIVED
     *                  \-> DRAFT
     */
    fun validateTransition(
        currentStatus: TherapyContentStatus,
        targetStatus: TherapyContentStatus,
    ): DataError.Conflict? {
        val allowedTargets =
            ALLOWED_TRANSITIONS[currentStatus].orEmpty()

        if (targetStatus in allowedTargets) {
            return null
        }

        return DataError.Conflict(
            message = "Therapy-content status transition is not allowed. " +
                    "currentStatus=$currentStatus, " +
                    "targetStatus=$targetStatus, " +
                    "allowedTargets=$allowedTargets."
        )
    }

    /**
     * Session metadata and owned modules may only be edited while the
     * aggregate is a draft.
     */
    fun validateContentMutationAllowed(
        session: TherapySession,
    ): DataError.Conflict? {
        if (session.status == TherapyContentStatus.DRAFT) {
            return null
        }

        return DataError.Conflict(
            message = "Therapy content cannot be modified in its current " +
                    "state. sessionId=${session.id}, " +
                    "status=${session.status}. " +
                    "Only DRAFT content is editable."
        )
    }

    /**
     * A session can only be permanently deleted before it enters the
     * review and publication workflow.
     *
     * Published content should be archived so existing downloads and
     * TherapySessionRuns continue referencing a valid content version.
     */
    fun validateDeletionAllowed(
        session: TherapySession,
    ): DataError.Conflict? {
        if (session.status == TherapyContentStatus.DRAFT) {
            return null
        }

        return DataError.Conflict(
            message = "Therapy content cannot be deleted in its current " +
                    "state. sessionId=${session.id}, " +
                    "status=${session.status}. " +
                    "Only DRAFT content may be deleted; published content " +
                    "must be archived."
        )
    }

    fun allowedTargets(
        currentStatus: TherapyContentStatus,
    ): Set<TherapyContentStatus> =
        ALLOWED_TRANSITIONS[currentStatus].orEmpty()
}
