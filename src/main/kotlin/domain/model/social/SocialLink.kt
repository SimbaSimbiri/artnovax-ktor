package com.simbiri.domain.model.social

data class SocialLink(
    val platform: SocialPlatform,
    val username: String,
    val completeUrl: String?,
)
