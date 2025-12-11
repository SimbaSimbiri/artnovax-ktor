package com.simbiri.data.mapper.user

import com.simbiri.data.database.entity.user.UserEntity
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.social.SocialLink
import com.simbiri.domain.model.user.User
import com.simbiri.domain.model.user.UserType
import java.time.Instant
import java.util.*

fun UserEntity.toDomain(
    socialLinks: List<SocialLink> = emptyList()
): User =
    User(
        id = UserId(id),
        accountName = accountName,
        emailAddress = emailAddress,
        firstName = firstName,
        lastName = lastName,
        birthDate = birthDate,
        about = about,
        tagline = tagline,
        profileUrl = profileUrl,
        backgroundUrl = backgroundUrl,
        type = UserType.fromCode(userTypeCode),
        emailOptIn = emailOptIn,
        isPrivate = isPrivate,
        isAnonymous = isAnonymous,
        isActive = isActive,
        socialLinks = socialLinks,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun User.toEntity(
    now: Instant = Instant.now(),
): UserEntity =
    UserEntity(
        id = this.id?.value ?: UUID.randomUUID(),
        accountName = accountName,
        emailAddress = emailAddress,
        firstName = firstName,
        lastName = lastName,
        birthDate = birthDate,
        about = about,
        tagline = tagline,
        profileUrl = profileUrl,
        backgroundUrl = backgroundUrl,
        userTypeCode = type.code,
        emailOptIn = emailOptIn,
        isPrivate = isPrivate,
        isAnonymous = isAnonymous,
        isActive = isActive,
        createdAt = createdAt.takeIf { this.id != null } ?: now,
        updatedAt = now,
    )
