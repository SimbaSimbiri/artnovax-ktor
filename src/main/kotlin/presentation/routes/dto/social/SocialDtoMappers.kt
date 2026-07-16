package com.simbiri.presentation.routes.dto.social

import com.simbiri.domain.model.social.SocialLink
import com.simbiri.domain.model.social.SocialPlatform
import com.simbiri.domain.model.social.SocialPlatformRegistry

fun SocialLink.toResponseDto(): SocialLinkResponseDto =
    SocialLinkResponseDto(
        platformName = platform.name,
        username = username,
        completeUrl = completeUrl,
    )

// req to domain mappers
fun SocialLinkUpsertDto.toDomain(): SocialLink? {
    val platform: SocialPlatform = SocialPlatformRegistry.byId[platformId]
        ?: return null

    val url = "${platform.baseUrl}${username}"

    return SocialLink(
        platform = platform,
        username = username,
        completeUrl = url,
    )
}