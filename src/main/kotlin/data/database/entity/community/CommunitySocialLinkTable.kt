package com.simbiri.data.database.entity.community

import com.simbiri.data.database.entity.social.SocialPlatformTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object CommunitySocialLinkTable : UUIDTable("community_social_links") {
    val communityId = uuid("community_id").references(CommunityTable.id)
    val platformId = integer("platform_id").references(SocialPlatformTable.id)
    val username = varchar("username", 80)
    val completeUrl = varchar("complete_url", 512)
    val createdAt = timestamp("created_at")

    init {
        // A user should not have duplicate socials on the same platform
        index(isUnique = true, columns = arrayOf(communityId, platformId, username))
    }
}