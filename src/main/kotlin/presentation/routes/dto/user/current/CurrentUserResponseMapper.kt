package com.simbiri.presentation.routes.dto.user.current

import com.simbiri.domain.model.user.User
import com.simbiri.presentation.routes.dto.social.toResponseDto

/**
 * Maps a persisted user to their complete authenticated profile response.
 */

fun User.toCurrentUserResponseDto(): CurrentUserResponseDto {
    val persistedUserId = requireNotNull(id) {
        "Current user is missing its persisted ID."
    }

    val persistedCreatedAt = requireNotNull(createdAt) {
        "Current user is missing createdAt. " + "userId=${persistedUserId.value}."
    }

    val persistedUpdatedAt = requireNotNull(updatedAt) {
        "Current user is missing updatedAt. " + "userId=${persistedUserId.value}."
    }

    return CurrentUserResponseDto(
        id = persistedUserId.value.toString(),
        accountName = accountName,
        emailAddress = emailAddress,
        firstName = firstName,
        lastName = lastName,
        birthDate = birthDate.toString(),

        about = about,
        tagline = tagline,

        profileImageUrl = profileUrl,
        backgroundImageUrl = backgroundUrl,

        userType = type.name,
        emailOptIn = emailOptIn,
        isPrivate = isPrivate,
        isAnonymous = isAnonymous,
        isActive = isActive,

        socialLinks = socialLinks.map { socialLink ->
            socialLink.toResponseDto()
        },

        createdAt = persistedCreatedAt.toString(),
        updatedAt = persistedUpdatedAt.toString(),
    )
}