package com.simbiri.presentation.routes.dto.user

import com.simbiri.presentation.routes.dto.social.SocialLinkUpsertDto
import kotlinx.serialization.Serializable

@Serializable
data class UserUpsertDto(
    val accountName: String,
    val emailAddress: String,
    val birthDate: String,
    val about: String? = null,
    val tagline: String? = null,
    val firstName: String,
    val lastName: String,
    val profileImageUrl: String? = null,
    val backgroundImageUrl: String? = null,
    val type: Int,
    val emailOptIn: Boolean = false,
    val isPrivate: Boolean = true,
    val isAnonymous: Boolean = false,
    val isActive: Boolean = true,
    val socialLinks: List<SocialLinkUpsertDto> = emptyList(),
)
