package com.simbiri.presentation.routes.dto.social

import kotlinx.serialization.Serializable

@Serializable
data class SocialLinkDto(
    val platformName: String,
    val username: String,
    val completeUrl: String?,
)