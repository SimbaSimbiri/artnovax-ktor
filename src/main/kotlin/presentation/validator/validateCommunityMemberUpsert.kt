package com.simbiri.presentation.validator

import com.simbiri.domain.model.community.ParticipantRole
import com.simbiri.presentation.routes.dto.community.CommunityMemberUpsertDto
import io.ktor.server.plugins.requestvalidation.*
import java.util.*


fun RequestValidationConfig.validateCommunityMemberUpsert() {
    validate<CommunityMemberUpsertDto> { dto ->

        val userUuid = runCatching { UUID.fromString(dto.userId) }.getOrNull()
            ?: return@validate ValidationResult.Invalid("userId must be a valid UUID.")

        val roleValid = runCatching {
            ParticipantRole.valueOf(dto.participantRole.uppercase())
        }.isSuccess

        if (!roleValid) {
            return@validate ValidationResult.Invalid("participantRole must be one of: OWNER, MODERATOR, MEMBER.")
        }

        ValidationResult.Valid
    }
}
