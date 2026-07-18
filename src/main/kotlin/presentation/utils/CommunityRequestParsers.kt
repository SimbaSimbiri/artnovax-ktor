package com.simbiri.presentation.utils

import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.community.Community
import com.simbiri.domain.model.community.CommunityMemberAssignment
import com.simbiri.domain.model.community.CommunityParticipantRole
import com.simbiri.domain.model.community.JoinPermission
import com.simbiri.domain.model.social.SocialPlatformRegistry
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.routes.dto.community.CommunityMemberRoleUpdateDto
import com.simbiri.presentation.routes.dto.community.CommunityMemberUpsertDto
import com.simbiri.presentation.routes.dto.community.CommunityUpsertDto
import com.simbiri.presentation.routes.dto.community.toAssignment
import com.simbiri.presentation.routes.dto.community.toDomainForCreate
import com.simbiri.presentation.routes.dto.community.toDomainForUpdate
import java.util.UUID

fun parseCommunityIdOrFailure(
    operation: String,
    rawCommunityId: String?,
): ResultType<CommunityId, DataError> {
    if (rawCommunityId.isNullOrBlank()) {
        return ResultType.Failure(
            validationError(
                operation = operation,
                field = "communityId",
                value = rawCommunityId,
                reason = "Community ID is required and cannot be blank."
            )
        )
    }

    val uuid = runCatching {
        UUID.fromString(rawCommunityId)
    }.getOrNull()
        ?: return ResultType.Failure(
            validationError(
                operation = operation,
                field = "communityId",
                value = rawCommunityId,
                reason = "Community ID must be a valid UUID."
            )
        )

    return ResultType.Success(
        CommunityId(uuid)
    )
}

fun parseOptionalCommunityOwnerIdOrFailure(
    operation: String,
    rawOwnerId: String?,
): ResultType<UserId?, DataError> {
    if (rawOwnerId == null) {
        return ResultType.Success(null)
    }

    return when (
        val parsed = parseUserIdOrFailure(
            operation = operation,
            rawUserId = rawOwnerId,
        )
    ) {
        is ResultType.Success ->
            ResultType.Success(parsed.data)

        is ResultType.Failure ->
            parsed
    }
}

fun CommunityUpsertDto.toCommunityForCreateOrFailure(
    operation: String,
): ResultType<Community, DataError> {
    val parsedFields = when (
        val parsed = parseCommunityRequestFields(
            operation = operation,
        )
    ) {
        is ResultType.Success ->
            parsed.data

        is ResultType.Failure ->
            return parsed
    }

    return ResultType.Success(
        toDomainForCreate(
            ownerId = parsedFields.ownerId,
            joinPermission = parsedFields.joinPermission,
        )
    )
}

fun CommunityUpsertDto.toCommunityForUpdateOrFailure(
    operation: String,
    communityId: CommunityId,
): ResultType<Community, DataError> {
    val parsedFields = when (
        val parsed = parseCommunityRequestFields(
            operation = operation,
        )
    ) {
        is ResultType.Success ->
            parsed.data

        is ResultType.Failure ->
            return parsed
    }

    return ResultType.Success(
        toDomainForUpdate(
            communityId = communityId,
            ownerId = parsedFields.ownerId,
            joinPermission = parsedFields.joinPermission,
        )
    )
}

fun CommunityMemberUpsertDto.toMemberAssignmentOrFailure(
    operation: String,
): ResultType<CommunityMemberAssignment, DataError> {
    val parsedUserId = when (
        val parsed = parseUserIdOrFailure(
            operation = operation,
            rawUserId = userId,
        )
    ) {
        is ResultType.Success ->
            parsed.data

        is ResultType.Failure ->
            return parsed
    }

    val parsedRole = when (
        val parsed = parseCommunityParticipantRoleOrFailure(
            operation = operation,
            rawRole = commParticipantRole,
        )
    ) {
        is ResultType.Success ->
            parsed.data

        is ResultType.Failure ->
            return parsed
    }

    return ResultType.Success(
        toAssignment(
            userId = parsedUserId,
            role = parsedRole,
        )
    )
}

fun CommunityMemberRoleUpdateDto.toMemberRoleOrFailure(
    operation: String,
): ResultType<CommunityParticipantRole, DataError> =
    parseCommunityParticipantRoleOrFailure(
        operation = operation,
        rawRole = commParticipantRole,
    )

private fun CommunityUpsertDto.parseCommunityRequestFields(
    operation: String,
): ResultType<ParsedCommunityRequestFields, DataError> {
    val parsedOwnerId = when (
        val parsed = parseUserIdOrFailure(
            operation = operation,
            rawUserId = ownerId,
        )
    ) {
        is ResultType.Success ->
            parsed.data

        is ResultType.Failure ->
            return parsed
    }

    val parsedJoinPermission =
        JoinPermission.fromCodeOrNull(joinPermission)
            ?: return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "joinPermission",
                    value = joinPermission.toString(),
                    reason = "Unsupported join-permission code. " +
                            "supportedCodes=" +
                            "${JoinPermission.entries.map { it.code }}."
                )
            )

    val unsupportedPlatformIndex =
        socialLinks.indexOfFirst { socialLink ->
            SocialPlatformRegistry.byId[socialLink.platformId] == null
        }

    if (unsupportedPlatformIndex >= 0) {
        val unsupportedPlatformId =
            socialLinks[unsupportedPlatformIndex].platformId

        return ResultType.Failure(
            validationError(
                operation = operation,
                field = "socialLinks[$unsupportedPlatformIndex].platformId",
                value = unsupportedPlatformId.toString(),
                reason = "Social-platform ID is not present in " +
                        "SocialPlatformRegistry."
            )
        )
    }

    return ResultType.Success(
        ParsedCommunityRequestFields(
            ownerId = parsedOwnerId,
            joinPermission = parsedJoinPermission,
        )
    )
}

private fun parseCommunityParticipantRoleOrFailure(
    operation: String,
    rawRole: String?,
): ResultType<CommunityParticipantRole, DataError> {
    if (rawRole.isNullOrBlank()) {
        return ResultType.Failure(
            validationError(
                operation = operation,
                field = "commParticipantRole",
                value = rawRole,
                reason = "Community participant role is required."
            )
        )
    }

    val role =
        CommunityParticipantRole.fromNameOrNull(rawRole)
            ?: return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "commParticipantRole",
                    value = rawRole,
                    reason = "Unsupported community participant role. " +
                            "supportedRoles=" +
                            "${CommunityParticipantRole.entries.map { it.name }}."
                )
            )

    return ResultType.Success(role)
}

private data class ParsedCommunityRequestFields(
    val ownerId: UserId,
    val joinPermission: JoinPermission,
)

private fun validationError(
    operation: String,
    field: String,
    value: String?,
    reason: String,
): DataError.ValidationError =
    DataError.ValidationError(
        message = "Community request validation failed in $operation. " +
                "field=$field, value=${value ?: "null"}. $reason"
    )