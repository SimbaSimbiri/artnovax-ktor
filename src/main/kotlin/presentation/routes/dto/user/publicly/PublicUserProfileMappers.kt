package com.simbiri.presentation.routes.dto.user.publicly

import com.simbiri.domain.model.user.User
import com.simbiri.domain.policy.user.UserProfileVisibilityPolicy
import com.simbiri.presentation.routes.dto.social.toResponseDto

/**
 * Maps one publicly visible User to its sanitized response.
 */
fun User.toPublicProfileResponseDto(): PublicUserProfileResponseDto {
    check(
        UserProfileVisibilityPolicy.isPubliclyVisible(this)
    ) {
        "A private or inactive user cannot be mapped to a public " + "profile. userId=${id?.value}."
    }

    val persistedUserId = requireNotNull(id) {
        "Persisted public user is missing its ID."
    }

    val displayName = if (UserProfileVisibilityPolicy.canExposeRealName(this)) {
        "$firstName $lastName"
    } else {
        null
    }

    val publicSocialLinks = if (UserProfileVisibilityPolicy.canExposeSocialLinks(this)) {
        socialLinks.map { socialLink ->
            socialLink.toResponseDto()
        }
    } else {
        emptyList()
    }

    return PublicUserProfileResponseDto(
        id = persistedUserId.value.toString(),
        accountName = accountName,
        displayName = displayName,
        profileImageUrl = profileUrl,
        backgroundImageUrl = backgroundUrl,
        tagline = tagline,
        about = about,
        userType = type.name,
        isAnonymous = isAnonymous,
        socialLinks = publicSocialLinks,
    )
}

/**
 * Maps a public profile collection while retaining repository order.
 */
fun List<User>.toPublicProfileResponseDtos(): List<PublicUserProfileResponseDto> = map(User::toPublicProfileResponseDto)