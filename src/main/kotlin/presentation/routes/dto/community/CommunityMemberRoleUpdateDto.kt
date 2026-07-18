package com.simbiri.presentation.routes.dto.community

import kotlinx.serialization.Serializable

/**
 * Request body for changing the role of an existing community member.
 *
 * The user ID comes from the HTTP path and therefore is intentionally
 * excluded from this DTO.
 */
@Serializable
data class CommunityMemberRoleUpdateDto(
    val commParticipantRole: String,
)