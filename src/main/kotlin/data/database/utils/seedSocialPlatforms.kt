package com.simbiri.data.database.utils

import com.simbiri.data.database.entity.social.SocialPlatformTable
import com.simbiri.domain.model.social.SocialPlatformRegistry
import org.jetbrains.exposed.sql.insertIgnore

fun seedSocialPlatforms() {
    SocialPlatformRegistry.byId.values.forEach { platform ->
        SocialPlatformTable.insertIgnore { row ->
            row[SocialPlatformTable.id] = platform.id
            row[name] = platform.name
            row[baseUrl] = platform.baseUrl
        }
    }

}
