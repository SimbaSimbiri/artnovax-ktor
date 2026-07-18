package com.simbiri.data.mapper.community

import com.simbiri.data.database.entity.community.CommunitySocialLinkEntity
import com.simbiri.domain.model.social.SocialLink
import com.simbiri.domain.model.social.SocialPlatformRegistry
import java.time.Instant
import java.util.UUID

fun CommunitySocialLinkEntity.toDomain(): SocialLink {
    val platform = SocialPlatformRegistry.byId[platformId]
        ?: throw IllegalStateException(
            "Cannot map CommunitySocialLinkEntity to domain. " +
                    "SocialPlatformRegistry does not contain " +
                    "platformId=$platformId. " +
                    "communityId=$communityId, socialLinkId=$id."
        )

    return SocialLink(
        platform = platform,
        username = username,
        completeUrl = completeUrl,
    )
}

fun SocialLink.toCommunitySocialLinkEntity(
    communityId: UUID,
    createdAt: Instant,
): CommunitySocialLinkEntity =
    CommunitySocialLinkEntity(
        id = UUID.randomUUID(),
        communityId = communityId,
        platformId = platform.id,
        username = username,
        completeUrl = completeUrl,
        createdAt = createdAt,
    )