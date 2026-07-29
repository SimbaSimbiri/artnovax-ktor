package com.simbiri.presentation.routes.dto.user.current

import com.simbiri.presentation.routes.dto.social.SocialLinkUpsertDto
import kotlinx.serialization.Serializable

/**
 * Editable current-user profile fields.
 *
 * Account name, email, role, active status, and user ID are excluded.
 */
@Serializable
data class CurrentUserUpdateRequestDto(
    val firstName: String,
    val lastName: String,
    val birthDate: String,

    val about: String? = null,
    val tagline: String? = null,

    val profileImageUrl: String? = null,
    val backgroundImageUrl: String? = null,

    val emailOptIn: Boolean = false,
    val isPrivate: Boolean = true,
    val isAnonymous: Boolean = false,

    val socialLinks: List<SocialLinkUpsertDto> = emptyList(),
)
