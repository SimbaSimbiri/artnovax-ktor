package com.simbiri.data.mapper.social

import com.simbiri.data.database.entity.social.SocialPlatformEntity
import com.simbiri.domain.model.social.SocialPlatform

fun SocialPlatformEntity.toDomain(): SocialPlatform =
    SocialPlatform(
        id = id,
        name = name,
        baseUrl = baseUrl,
    )

fun SocialPlatform.toEntity(): SocialPlatformEntity =
    SocialPlatformEntity(
        id = id,
        name = name,
        baseUrl = baseUrl,
    )