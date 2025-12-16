package com.simbiri.data.database.entity.community

import com.simbiri.data.database.entity.user.UserTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object CommunityMemberTable: Table("community_members") {
    val userId = uuid("user_id").references(UserTable.id)
    val communityId = uuid("community_id").references(CommunityTable.id)
    val joinedAt = timestamp("joined_at")
    val leftAt = timestamp("left_at").nullable()
    val userTypeAtJoin = integer("user_type_at_join").nullable()
    val participantRole = varchar("participant_role", 16)

    override val primaryKey= PrimaryKey(userId, communityId)

}