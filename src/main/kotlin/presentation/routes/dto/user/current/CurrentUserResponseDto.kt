package com.simbiri.presentation.routes.dto.user.current

import com.simbiri.presentation.routes.dto.social.SocialLinkResponseDto
import kotlinx.serialization.Serializable

/**
 * Complete non-credential profile returned to the authenticated user.
 */
@Serializable
data class CurrentUserResponseDto(
    val id: String,

    val accountName: String,
    val emailAddress: String,

    val firstName: String,
    val lastName: String,
    val birthDate: String,

    val about: String?,
    val tagline: String?,

    val profileImageUrl: String?,
    val backgroundImageUrl: String?,

    val userType: String,

    val emailOptIn: Boolean,
    val isPrivate: Boolean,
    val isAnonymous: Boolean,
    val isActive: Boolean,

    val socialLinks: List<SocialLinkResponseDto>,

    val createdAt: String,
    val updatedAt: String,
)