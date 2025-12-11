package com.simbiri.data.database.utils

import com.simbiri.domain.model.social.SocialPlatformRegistry
import com.simbiri.data.database.entity.social.SocialPlatformTable
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction

fun seedSocialPlatforms() {
    transaction {
        SocialPlatformRegistry.byId.values.forEach { platform ->
            SocialPlatformTable.insertIgnore { row ->
                row[SocialPlatformTable.id] = platform.id
                row[name] = platform.name
                row[baseUrl] = platform.baseUrl
            }
        }
    }
}
