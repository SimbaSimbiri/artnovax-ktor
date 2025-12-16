package com.simbiri.data.mapper.community

import com.simbiri.data.database.entity.community.CommunityEntity
import com.simbiri.data.database.entity.community.CommunityMemberEntity
import com.simbiri.data.database.entity.community.CommunityMemberTable
import com.simbiri.data.database.entity.community.CommunitySocialLinkEntity
import com.simbiri.data.database.entity.community.CommunitySocialLinkTable
import com.simbiri.data.database.entity.community.CommunityTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toCommunityEntity(): CommunityEntity =
    CommunityEntity(
        id = this[CommunityTable.id].value,
        ownerId = this[CommunityTable.ownerId],
        name = this[CommunityTable.name],
        description = this[CommunityTable.description],
        profileUrl = this[CommunityTable.profileUrl],
        memberCount = this[CommunityTable.memberCount],
        joinPermissionCode = this[CommunityTable.joinPermission],
        chatBackgroundUrl = this[CommunityTable.chatBackgroundUrl],
        tagline = this[CommunityTable.tagline],
        privateEvents = this[CommunityTable.privateEvents],
        privatePosts = this[CommunityTable.privatePosts],
        category = this[CommunityTable.category],
        approved = this[CommunityTable.approved],
        createdAt = this[CommunityTable.createdAt],
        updatedAt = this[CommunityTable.updatedAt],
    )

fun ResultRow.toCommunityMemberEntity(): CommunityMemberEntity =
    CommunityMemberEntity(
        userId = this[CommunityMemberTable.userId],
        communityId = this[CommunityMemberTable.communityId],
        joinedAt = this[CommunityMemberTable.joinedAt],
        leftAt = this[CommunityMemberTable.leftAt],
        userTypeAtJoinCode = this[CommunityMemberTable.userTypeAtJoin],
        participantRole = this[CommunityMemberTable.participantRole],
    )

fun ResultRow.toCommunitySocialLinkEntity(): CommunitySocialLinkEntity =
    CommunitySocialLinkEntity(
        id = this[CommunitySocialLinkTable.id].value,
        communityId = this[CommunitySocialLinkTable.communityId],
        platformId = this[CommunitySocialLinkTable.platformId],
        username = this[CommunitySocialLinkTable.username],
        completeUrl = this[CommunitySocialLinkTable.completeUrl],
        createdAt = this[CommunitySocialLinkTable.createdAt],
    )
