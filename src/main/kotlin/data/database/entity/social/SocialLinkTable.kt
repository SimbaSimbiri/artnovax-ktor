package com.simbiri.data.database.entity.social

import com.simbiri.data.database.entity.user.UserTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object SocialLinkTable : UUIDTable("social_links") {
    val userId = reference("user_id", UserTable) // FK → users.id
    val platformId = reference("platform_id", SocialPlatformTable) // FK → social_platforms.id
    val username = varchar("username", 255)
    val completeUrl = varchar("complete_url", 512)
    val createdAt = timestamp("created_at")

    init {
        // A user should not have duplicate socials on the same platform
        index(isUnique = true, columns = arrayOf(userId, platformId, username))
    }
}