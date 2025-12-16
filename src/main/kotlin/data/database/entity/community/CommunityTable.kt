package com.simbiri.data.database.entity.community

import com.simbiri.data.database.entity.user.UserTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object CommunityTable : UUIDTable(name = "Communities") {
    val ownerId = uuid("owner_id").references(UserTable.id)
    val name =  varchar("name", 150)
    val description= text("description")
    val profileUrl= varchar("profile_url", 512).nullable()
    val memberCount = integer("member_count").default(0)
    val joinPermission= integer("join_permission").default(0)
    val chatBackgroundUrl= varchar("chat_background_url", 512).nullable()
    val tagline= varchar("tagline", 255)
    val privateEvents= bool("private_events").default(true)
    val privatePosts= bool("private_posts").default(true)
    val category = varchar("category", 100).nullable()
    val approved= bool("approved").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}