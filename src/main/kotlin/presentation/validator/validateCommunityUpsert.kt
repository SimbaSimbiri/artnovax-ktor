package com.simbiri.presentation.validator

import com.simbiri.domain.model.community.JoinPermission
import com.simbiri.domain.model.social.SocialPlatformRegistry
import com.simbiri.presentation.routes.dto.community.CommunityUpsertDto
import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import java.util.UUID

fun RequestValidationConfig.validateCommunityUpsert() {
    validate<CommunityUpsertDto> { dto ->
        validateCommunityRequestShape(dto)
            ?.let(ValidationResult::Invalid)
            ?: ValidationResult.Valid
    }

    validate<List<CommunityUpsertDto>> { communities ->
        val firstError =
            communities.withIndex().firstNotNullOfOrNull { (index, dto) ->
                validateCommunityRequestShape(dto)?.let { reason ->
                    "communities[$index]: $reason"
                }
            }

        firstError
            ?.let(ValidationResult::Invalid)
            ?: ValidationResult.Valid
    }
}

private fun validateCommunityRequestShape(
    dto: CommunityUpsertDto,
): String? {
    if (
        runCatching {
            UUID.fromString(dto.ownerId)
        }.getOrNull() == null
    ) {
        return "ownerId must be a valid UUID."
    }

    if (
        JoinPermission.fromCodeOrNull(
            dto.joinPermission
        ) == null
    ) {
        return "joinPermission '${dto.joinPermission}' is unsupported. " +
                "Supported codes are " +
                "${JoinPermission.entries.map { it.code }}."
    }

    val invalidPlatformIndex =
        dto.socialLinks.indexOfFirst { socialLink ->
            SocialPlatformRegistry.byId[socialLink.platformId] == null
        }

    if (invalidPlatformIndex >= 0) {
        val invalidPlatformId =
            dto.socialLinks[invalidPlatformIndex].platformId

        return "socialLinks[$invalidPlatformIndex].platformId " +
                "'$invalidPlatformId' is not present in the " +
                "social-platform registry."
    }

    return null
}