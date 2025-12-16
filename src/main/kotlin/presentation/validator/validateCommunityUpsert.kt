package com.simbiri.presentation.validator

import com.simbiri.presentation.routes.dto.community.CommunityUpsertDto
import io.ktor.server.plugins.requestvalidation.*
import java.util.*

private val ALLOWED_JOIN_PERMISSIONS = setOf(0,1)
private val ALLOWED_SOCIAL_PLATFORM_IDS = setOf(1, 2, 3, 4, 5)

fun RequestValidationConfig.validateCommunityUpsert() {
    validate<CommunityUpsertDto> { dto ->

        val ownerUuid = runCatching { UUID.fromString(dto.ownerId) }.getOrNull()
            ?: return@validate ValidationResult.Invalid("ownerId must be a valid UUID.")

        when {
            dto.name.isBlank() || dto.name.length !in 3..120 -> {
                ValidationResult.Invalid("name must be between 3 and 120 characters.")
            }

            dto.description.isBlank() || dto.description.length < 10 -> {
                ValidationResult.Invalid("description must be at least 10 characters.")
            }

            dto.tagline.isBlank() || dto.tagline.length > 255 -> {
                ValidationResult.Invalid("tagline is required and must be at most 255 characters.")
            }

            dto.category != null && dto.category.length > 80 -> {
                ValidationResult.Invalid("category must be at most 80 characters.")
            }

            dto.joinPermission !in ALLOWED_JOIN_PERMISSIONS -> {
                ValidationResult.Invalid("joinPermissionCode must be one of: AUTO, APPROVAL.")
            }

            dto.socialLinks.any { it.username.isBlank() } -> {
                ValidationResult.Invalid("Each social link must have a non-empty username.")
            }

            dto.socialLinks.any { it.platformId !in ALLOWED_SOCIAL_PLATFORM_IDS } -> {
                ValidationResult.Invalid("One or more Social Links are not in Platform registry.")
            }

            else -> ValidationResult.Valid
        }
    }
}
