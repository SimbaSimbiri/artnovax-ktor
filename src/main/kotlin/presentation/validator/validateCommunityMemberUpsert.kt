package com.simbiri.presentation.validator

import com.simbiri.domain.model.community.CommunityParticipantRole
import com.simbiri.presentation.routes.dto.community.CommunityMemberUpsertDto
import io.ktor.server.plugins.requestvalidation.*
import java.util.*


fun RequestValidationConfig.validateCommunityMemberUpsert() {
    validate<CommunityMemberUpsertDto> { dto ->

        val userUuid = runCatching { UUID.fromString(dto.userId) }.getOrNull()
            ?: return@validate ValidationResult.Invalid("userId must be a valid UUID.")

        val roleValid = runCatching {
            CommunityParticipantRole.valueOf(dto.commParticipantRole.uppercase())
        }.isSuccess

        if (!roleValid) {
            return@validate ValidationResult.Invalid("commParticipantRole must be one of: OWNER, MODERATOR, MEMBER.")
        }

        ValidationResult.Valid
    }
}
