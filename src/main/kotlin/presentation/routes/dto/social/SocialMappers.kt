package com.simbiri.presentation.routes.dto.social

import com.simbiri.domain.model.social.SocialLink

fun SocialLink.toDto(): SocialLinkDto =
    SocialLinkDto(
        platformName = platform.name,
        username = username,
        completeUrl = completeUrl,
    )
