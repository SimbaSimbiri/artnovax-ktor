package com.simbiri.presentation.routes.dto.social

import com.simbiri.domain.model.social.SocialLink
import com.simbiri.domain.model.social.SocialPlatform

fun SocialLink.toResponseDto(): SocialLinkResponseDto =
    SocialLinkResponseDto(
        platformName = platform.name,
        username = username,
        completeUrl = completeUrl,
    )

// req to domain mappers
fun SocialLinkUpsertDto.toDomain(
    platformsById: Map<Int, SocialPlatform>,
): SocialLink? {
    val platform = platformsById[platformId] ?: return null
    val url = "${platform.baseUrl}/${username}".trimEnd('/')
    return SocialLink(
        platform = platform,
        username = username,
        completeUrl = url,
    )
}