package com.simbiri.data.database.entity.social

import org.jetbrains.exposed.dao.id.IntIdTable

object SocialPlatformTable : IntIdTable("social_platforms") {
    val name = varchar("name", 50).uniqueIndex()
    val baseUrl = varchar("base_url", 255)
}
