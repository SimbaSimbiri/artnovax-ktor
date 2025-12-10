package com.simbiri.presentation.routes.dto.user

import com.simbiri.domain.model.user.User
import com.simbiri.presentation.routes.dto.social.toDto


fun User.toResponseDto(): UserDto {
    val canShowSocials = canExposeSocialLinks && !isAnonymous

    val socialDtos = if (canShowSocials) {
        socialLinks.map { it.toDto() }
    } else {
        emptyList()
    }

    return UserDto(
        id = id.value.toString(),
        accountName = accountName,
        emailAddress = emailAddress,
        fullName = "$firstName $lastName",
        profileImageUrl = profileUrl,
        backgroundImageUrl = backgroundUrl,
        tagline = tagline,
        type = type.code,
        isAnonymous = isAnonymous,
        isActive = isActive,
        isPrivate = isPrivate,
        emailOptIn = emailOptIn,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
        socialLinks = socialDtos,
    )
}