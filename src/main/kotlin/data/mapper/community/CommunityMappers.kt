package com.simbiri.data.mapper.community

import com.simbiri.data.database.entity.community.CommunityEntity
import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.community.Community
import com.simbiri.domain.model.community.JoinPermission
import com.simbiri.domain.model.social.SocialLink
import java.time.Instant
import java.util.UUID

fun CommunityEntity.toDomain(
    socialLinks: List<SocialLink> = emptyList(),
): Community =
    Community(
        id = CommunityId(id),
        ownerId = UserId(ownerId),
        name = name,
        description = description,
        profileUrl = profileUrl,
        memberCount = memberCount,
        joinPermission = JoinPermission.fromCode(joinPermissionCode),
        chatBackgroundUrl = chatBackgroundUrl,
        tagline = tagline,
        privateEvents = privateEvents,
        privatePosts = privatePosts,
        category = category,
        approved = approved,
        socialLinks = socialLinks,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

/**
 * Maps a community into its persistence representation.
 *
 * The caller supplies the transaction timestamp explicitly. This prevents
 * the mapper from reading the system clock independently of the repository.
 */
fun Community.toEntity(
    now: Instant,
): CommunityEntity =
    CommunityEntity(
        id = id?.value ?: UUID.randomUUID(),
        ownerId = ownerId.value,
        name = name,
        description = description,
        profileUrl = profileUrl,
        memberCount = memberCount,
        joinPermissionCode = joinPermission.code,
        chatBackgroundUrl = chatBackgroundUrl,
        tagline = tagline,
        privateEvents = privateEvents,
        privatePosts = privatePosts,
        category = category,
        approved = approved,
        createdAt = createdAt ?: now,
        updatedAt = now,
    )