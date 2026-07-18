package com.simbiri.presentation.routes.dto.user

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.social.SocialLink
import com.simbiri.domain.model.user.User
import com.simbiri.domain.model.user.UserType
import com.simbiri.presentation.routes.dto.social.toDomain
import com.simbiri.presentation.routes.dto.social.toResponseDto
import java.time.LocalDate
import java.util.UUID

/**
 * Maps a persisted domain user to its API response representation.
 *
 * A user returned by the repository must contain an ID and persistence
 * timestamps. Missing values indicate an internal lifecycle invariant
 * violation.
 */
fun User.toResponseDto(): UserResponseDto {
    val persistedUserId = requireNotNull(id) {
        "Cannot map user to UserResponseDto because user.id is null. " +
                "accountName=$accountName, emailAddress=$emailAddress."
    }

    val persistedCreatedAt = requireNotNull(createdAt) {
        "Cannot map user to UserResponseDto because user.createdAt is null. " +
                "userId=${persistedUserId.value}, accountName=$accountName."
    }

    val persistedUpdatedAt = requireNotNull(updatedAt) {
        "Cannot map user to UserResponseDto because user.updatedAt is null. " +
                "userId=${persistedUserId.value}, accountName=$accountName."
    }

    val canShowSocialLinks =
        canExposeSocialLinks && !isAnonymous

    val socialResponseDtos = if (canShowSocialLinks) {
        socialLinks.map { socialLink ->
            socialLink.toResponseDto()
        }
    } else {
        emptyList()
    }

    return UserResponseDto(
        id = persistedUserId.value.toString(),
        accountName = accountName,
        emailAddress = emailAddress,
        fullName = "$firstName $lastName",
        profileImageUrl = profileUrl,
        backgroundImageUrl = backgroundUrl,
        tagline = tagline,
        about = about,
        type = type.code,
        isAnonymous = isAnonymous,
        isActive = isActive,
        isPrivate = isPrivate,
        emailOptIn = emailOptIn,
        createdAt = persistedCreatedAt.toString(),
        updatedAt = persistedUpdatedAt.toString(),
        socialLinks = socialResponseDtos,
    )
}

fun List<User>.toResponseDto(): List<UserResponseDto> =
    map { user ->
        user.toResponseDto()
    }

/**
 * Maps a create-request DTO into an unpersisted domain user.
 *
 * IDs and persistence timestamps are intentionally absent.
 */
fun UserUpsertDto.toDomainForCreate(): User =
    toDomainInternal(
        existingUserId = null,
    )

fun List<UserUpsertDto>.toDomainForCreate(): List<User> =
    map { dto ->
        dto.toDomainForCreate()
    }

/**
 * Maps an update-request DTO into a domain user carrying the ID parsed from
 * the HTTP path.
 *
 * Persistence timestamps remain absent because they are not supplied by the
 * client and are managed by the repository.
 */
fun UserUpsertDto.toDomainForUpdate(
    userId: UUID,
): User =
    toDomainInternal(
        existingUserId = userId,
    )

private fun UserUpsertDto.toDomainInternal(
    existingUserId: UUID?,
): User {
    val parsedBirthDate = LocalDate.parse(birthDate)
    val parsedUserType = UserType.fromCode(type)

    val domainSocialLinks: List<SocialLink> =
        socialLinks.mapNotNull { socialLinkDto ->
            socialLinkDto.toDomain()
        }

    return User(
        id = existingUserId?.let(::UserId),
        accountName = accountName,
        emailAddress = emailAddress,
        birthDate = parsedBirthDate,
        about = about,
        tagline = tagline,
        firstName = firstName,
        lastName = lastName,
        profileUrl = profileImageUrl,
        backgroundUrl = backgroundImageUrl,
        type = parsedUserType,
        emailOptIn = emailOptIn,
        isPrivate = isPrivate,
        isAnonymous = isAnonymous,
        isActive = isActive,
        socialLinks = domainSocialLinks,
        createdAt = null,
        updatedAt = null,
    )
}