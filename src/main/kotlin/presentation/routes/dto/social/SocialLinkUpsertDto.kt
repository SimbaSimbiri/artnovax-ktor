package com.simbiri.presentation.routes.dto.social

import kotlinx.serialization.Serializable

@Serializable
data class SocialLinkUpsertDto(
    val platformId: Int,
    val username: String,
)
