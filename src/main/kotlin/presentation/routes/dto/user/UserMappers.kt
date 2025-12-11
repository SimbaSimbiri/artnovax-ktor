package com.simbiri.presentation.routes.dto.user

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.social.SocialLink
import com.simbiri.domain.model.social.SocialPlatform
import com.simbiri.domain.model.social.SocialPlatformRegistry
import com.simbiri.domain.model.user.User
import com.simbiri.domain.model.user.UserType
import com.simbiri.presentation.routes.dto.social.SocialLinkUpsertDto
import com.simbiri.presentation.routes.dto.social.toResponseDto
import java.time.Instant
import java.time.LocalDate
import java.util.UUID


fun User.toResponseDto(): UserResponseDto {
    val canShowSocials = canExposeSocialLinks && !isAnonymous

    val socialDtos = if (canShowSocials) {
        socialLinks.map { it.toResponseDto() }
    } else {
        emptyList()
    }

    return UserResponseDto(
        id = id?.value.toString(),
        accountName = accountName,
        emailAddress = emailAddress,
        fullName = "$firstName $lastName",
        profileImageUrl = profileUrl,
        backgroundImageUrl = backgroundUrl,
        tagline = tagline,
        about= about,
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

fun List<User>.toResponseDto() = this.map { it.toResponseDto() }

// added helpers for request DTOs
fun UserUpsertDto.toDomainForCreate(
    now: Instant = Instant.now(),
): User =
    toDomainInternal(
        now = now,
        existingUserId = null,
    )
fun List<UserUpsertDto>.toDomainForCreate() = this.map { it.toDomainForCreate() }

fun UserUpsertDto.toDomainForUpdate(
    userId: UUID,
    now: Instant = Instant.now(),
): User =
    toDomainInternal(
        now = now,
        existingUserId = userId,
    )

private fun UserUpsertDto.toDomainInternal(
    now: Instant,
    existingUserId: UUID?,
): User {
    val birth = LocalDate.parse(birthDate)
    val typeEnum = UserType.fromCode(type)

    val socialLinksDomain: List<SocialLink> =
        socialLinks.mapNotNull { it.toDomain() }

    return User(
        id = existingUserId?.let { UserId(it) },
        accountName = accountName,
        emailAddress = emailAddress,
        birthDate = birth,
        about = about,
        tagline = tagline,
        firstName = firstName,
        lastName = lastName,
        profileUrl = profileImageUrl,
        backgroundUrl = backgroundImageUrl,
        type = typeEnum,
        emailOptIn = emailOptIn,
        isPrivate = isPrivate,
        isAnonymous = isAnonymous,
        isActive = isActive,
        socialLinks = socialLinksDomain,
        createdAt = now,
        updatedAt = now,
    )
}

private fun SocialLinkUpsertDto.toDomain(): SocialLink? {
    val platform: SocialPlatform = SocialPlatformRegistry.byId[platformId]
        ?: return null

    val url = "${platform.baseUrl}${username}"

    return SocialLink(
        platform = platform,
        username = username,
        completeUrl = url,
    )
}