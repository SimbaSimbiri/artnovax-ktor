package com.simbiri.presentation.routes.dto.user

import com.simbiri.presentation.routes.dto.social.SocialLinkResponseDto
import kotlinx.serialization.Serializable

@Serializable
data class UserResponseDto(
    val id: String,
    val accountName: String,
    val emailAddress: String,
    val fullName: String,
    val profileImageUrl: String?,
    val backgroundImageUrl: String?,
    val tagline: String?,
    val type: Int,
    val isAnonymous: Boolean,
    val isActive: Boolean,
    val isPrivate: Boolean,
    val emailOptIn: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val socialLinks: List<SocialLinkResponseDto>,
)