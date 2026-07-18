package com.simbiri.presentation.validator

import com.simbiri.domain.model.community.CommunityParticipantRole
import com.simbiri.presentation.routes.dto.community.CommunityMemberRoleUpdateDto
import com.simbiri.presentation.routes.dto.community.CommunityMemberUpsertDto
import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import java.util.UUID

fun RequestValidationConfig.validateCommunityMemberUpsert() {
    validate<CommunityMemberUpsertDto> { dto ->
        validateMemberCreateRequestShape(dto)
            ?.let(ValidationResult::Invalid)
            ?: ValidationResult.Valid
    }

    validate<List<CommunityMemberUpsertDto>> { members ->
        val firstError =
            members.withIndex().firstNotNullOfOrNull { (index, dto) ->
                validateMemberCreateRequestShape(dto)?.let { reason ->
                    "members[$index]: $reason"
                }
            }

        firstError
            ?.let(ValidationResult::Invalid)
            ?: ValidationResult.Valid
    }

    validate<CommunityMemberRoleUpdateDto> { dto ->
        validateRole(dto.commParticipantRole)
            ?.let(ValidationResult::Invalid)
            ?: ValidationResult.Valid
    }
}

private fun validateMemberCreateRequestShape(
    dto: CommunityMemberUpsertDto,
): String? {
    if (
        runCatching {
            UUID.fromString(dto.userId)
        }.getOrNull() == null
    ) {
        return "userId must be a valid UUID."
    }

    return validateRole(dto.commParticipantRole)
}

private fun validateRole(
    rawRole: String,
): String? {
    if (
        CommunityParticipantRole.fromNameOrNull(rawRole) == null
    ) {
        return "commParticipantRole '$rawRole' is unsupported. " +
                "Supported roles are " +
                "${CommunityParticipantRole.entries.map { it.name }}."
    }

    return null
}