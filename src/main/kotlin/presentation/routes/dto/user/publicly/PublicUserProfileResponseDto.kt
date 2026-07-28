package com.simbiri.presentation.routes.dto.user.publicly

import com.simbiri.presentation.routes.dto.social.SocialLinkResponseDto
import kotlinx.serialization.Serializable

/**
 * Information safe to expose through unauthenticated profile endpoints.
 *
 * Email, birthdate, privacy settings, account status, and persistence
 * timestamps are excluded.
 */
@Serializable
data class PublicUserProfileResponseDto(
    val id: String,
    val accountName: String,

    val displayName: String?,

    val profileImageUrl: String?,
    val backgroundImageUrl: String?,

    val tagline: String?,
    val about: String?,

    val userType: String,
    val isAnonymous: Boolean,

    val socialLinks:
    List<SocialLinkResponseDto>,
)