package com.simbiri.data.mapper.social

import com.simbiri.data.database.entity.social.SocialLinkEntity
import com.simbiri.data.database.entity.social.SocialLinkTable
import com.simbiri.data.database.entity.social.SocialPlatformEntity
import com.simbiri.data.database.entity.social.SocialPlatformTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toSocialPlatformEntity(): SocialPlatformEntity =
    SocialPlatformEntity(
        id = this[SocialPlatformTable.id].value,
        name = this[SocialPlatformTable.name],
        baseUrl = this[SocialPlatformTable.baseUrl],
    )

fun ResultRow.toSocialLinkEntity(): SocialLinkEntity =
    SocialLinkEntity(
        id = this[SocialLinkTable.id].value,
        userId = this[SocialLinkTable.userId].value,
        platformId = this[SocialLinkTable.platformId].value,
        username = this[SocialLinkTable.username],
        completeUrl = this[SocialLinkTable.completeUrl],
        createdAt = this[SocialLinkTable.createdAt],
    )