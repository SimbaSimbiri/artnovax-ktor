package com.simbiri.domain.policy.community

import com.simbiri.domain.model.community.CommunityMemberAssignment
import com.simbiri.domain.model.community.CommunityParticipantRole
import com.simbiri.domain.util.DataError

/**
 * Contains pure rules for community membership commands.
 *
 * Database-dependent rules remain in the repository, including:
 * - whether the user exists;
 * - whether the community exists;
 * - whether another owner already exists;
 * - whether the membership already exists.
 */
object CommunityMembershipPolicy {

    fun validateBulkAssignments(
        assignments: List<CommunityMemberAssignment>,
    ): DataError.ValidationError? {
        val duplicateUser =
            assignments
                .groupingBy { assignment ->
                    assignment.userId
                }
                .eachCount()
                .entries
                .firstOrNull { (_, count) ->
                    count > 1
                }

        if (duplicateUser != null) {
            return validationError(
                field = "assignments",
                value = duplicateUser.key.value.toString(),
                reason = "Bulk membership payload cannot contain the same " +
                        "user more than once."
            )
        }

        val ownerCount = assignments.count { assignment ->
            assignment.role == CommunityParticipantRole.OWNER
        }

        if (ownerCount > 1) {
            return validationError(
                field = "assignments",
                value = "ownerCount=$ownerCount",
                reason = "Bulk membership payload cannot contain more than " +
                        "one OWNER assignment."
            )
        }

        return null
    }

    private fun validationError(
        field: String,
        value: Any?,
        reason: String,
    ): DataError.ValidationError =
        DataError.ValidationError(
            message = "Community-membership validation failed. " +
                    "field=$field, value=$value. $reason"
        )
}