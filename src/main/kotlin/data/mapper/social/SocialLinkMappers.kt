package com.simbiri.data.mapper.social

import com.simbiri.data.database.entity.social.SocialLinkEntity
import com.simbiri.domain.model.social.SocialLink
import com.simbiri.domain.model.social.SocialPlatform
import java.time.Instant
import java.util.*

fun SocialLinkEntity.toDomain(
    platform: SocialPlatform,
): SocialLink =
    SocialLink(
        platform = platform,
        username = username,
        completeUrl = completeUrl,
    )


fun SocialLink.toEntity(
    userId: UUID,
    platformId: Int,
    createdAt: Instant = Instant.now(),
): SocialLinkEntity =
    SocialLinkEntity(
        id = UUID.randomUUID(),
        userId = userId,
        platformId = platformId,
        username = username,
        completeUrl = completeUrl,
        createdAt = createdAt,
    )