package com.simbiri.domain.policy.therapy

import com.simbiri.domain.model.therapy.TherapySession
import com.simbiri.domain.model.user.User
import com.simbiri.domain.util.DataError

/**
 * Pure authorization rules for authored therapy content.
 *
 * Authentication and JWT parsing belong to presentation. This policy
 * receives an already-resolved User and determines whether that user may
 * perform a domain operation.
 */
object TherapyContentAccessPolicy {

    fun validateCanCreateDraft(
        actor: User,
        session: TherapySession,
    ): DataError.Forbidden? =
        validateCanManageDraft(
            actor = actor,
            session = session,
            operation = "create therapy content",
        )

    /**
     * Authorizes who can update and delete TherapySession drafts
     */
    fun validateCanManageDraft(
        actor: User,
        session: TherapySession,
        operation: String = "manage and update therapy content",
    ): DataError.Forbidden? {
        validateActiveActor(
            actor = actor,
            operation = operation,
        )?.let { error ->
            return error
        }

        if (!actor.canAuthorTherapyContent) {
            return forbidden(
                actor = actor,
                operation = operation,
                reason = "User type '${actor.type}' is not permitted to " +
                        "author therapy content."
            )
        }

        val actorId =
            actor.id
                ?: return forbidden(
                    actor = actor,
                    operation = operation,
                    reason = "A persisted actor ID is required."
                )

        if (actorId != session.authorId) {
            return forbidden(
                actor = actor,
                operation = operation,
                reason = "Only the recorded session author may modify this " +
                        "therapy-content draft. " +
                        "sessionAuthorId=${session.authorId.value}."
            )
        }

        return null
    }

    fun validateCanReview(
        actor: User,
        session: TherapySession,
    ): DataError.Forbidden? {
        validateActiveActor(
            actor = actor,
            operation = "review therapy content",
        )?.let { error ->
            return error
        }

        if (!actor.canReviewTherapyContent) {
            return forbidden(
                actor = actor,
                operation = "review therapy content",
                reason = "User type '${actor.type}' is not permitted to " +
                        "review therapy content."
            )
        }

        val actorId =
            actor.id
                ?: return forbidden(
                    actor = actor,
                    operation = "review therapy content",
                    reason = "A persisted reviewer ID is required."
                )

        if (actorId == session.authorId) {
            return forbidden(
                actor = actor,
                operation = "review therapy content",
                reason = "Therapy content requires independent review. " +
                        "The author cannot review their own session."
            )
        }

        return null
    }

    fun validateCanPublish(
        actor: User,
        session: TherapySession,
    ): DataError.Forbidden? {
        validateActiveActor(
            actor = actor,
            operation = "publish therapy content",
        )?.let { error ->
            return error
        }

        if (!actor.canPublishTherapyContent) {
            return forbidden(
                actor = actor,
                operation = "publish therapy content",
                reason = "User type '${actor.type}' is not permitted to " +
                        "publish therapy content."
            )
        }

        val actorId =
            actor.id
                ?: return forbidden(
                    actor = actor,
                    operation = "publish therapy content",
                    reason = "A persisted publisher ID is required."
                )

        if (actorId == session.authorId) {
            return forbidden(
                actor = actor,
                operation = "publish therapy content",
                reason = "Therapy content requires independent approval. " +
                        "The author cannot publish their own session."
            )
        }

        return null
    }

    fun validateCanArchive(
        actor: User,
    ): DataError.Forbidden? {
        validateActiveActor(
            actor = actor,
            operation = "archive therapy content",
        )?.let { error ->
            return error
        }

        if (!actor.canPublishTherapyContent) {
            return forbidden(
                actor = actor,
                operation = "archive therapy content",
                reason = "User type '${actor.type}' is not permitted to " +
                        "archive therapy content."
            )
        }

        return null
    }

    /**
     * Ensures user is active and persisted before authoring TherapyContent
     */
    private fun validateActiveActor(
        actor: User,
        operation: String,
    ): DataError.Forbidden? =
        when {
            actor.id == null -> {
                forbidden(
                    actor = actor,
                    operation = operation,
                    reason = "A persisted actor ID is required."
                )
            }

            !actor.isActive -> {
                forbidden(
                    actor = actor,
                    operation = operation,
                    reason = "Inactive users cannot perform therapy-content " +
                            "management operations."
                )
            }

            else -> null
        }

    private fun forbidden(
        actor: User,
        operation: String,
        reason: String,
    ): DataError.Forbidden =
        DataError.Forbidden(
            message = "Therapy-content authorization failed. " +
                    "operation=$operation, " +
                    "actorId=${actor.id?.value}, " +
                    "actorType=${actor.type}. $reason"
        )
}
