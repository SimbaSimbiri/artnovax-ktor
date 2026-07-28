package com.simbiri.presentation.routes.dto.auth

import com.simbiri.domain.model.user.User
import com.simbiri.domain.model.user.UserType
import java.time.LocalDate

/**
 * Maps a public registration request to an unpersisted REGULAR user.
 */
fun RegisterUserRequestDto.toRegistrationUser(): User = User(
    id = null,

    accountName = accountName,
    emailAddress = emailAddress,

    firstName = firstName,
    lastName = lastName,
    birthDate = LocalDate.parse(birthDate),

    about = about,
    tagline = tagline,
    profileUrl = profileImageUrl,
    backgroundUrl = backgroundImageUrl,

    type = UserType.REGULAR,

    emailOptIn = emailOptIn,
    isPrivate = isPrivate,
    isAnonymous = isAnonymous,
    isActive = true,

    socialLinks = emptyList(),

    createdAt = null,
    updatedAt = null,
)
