package com.simbiri.presentation.routes.dto.community

import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.community.Community
import com.simbiri.domain.model.community.CommunityMember
import com.simbiri.domain.model.community.JoinPermission
import com.simbiri.domain.model.community.ParticipantRole
import com.simbiri.domain.model.social.SocialLink
import com.simbiri.presentation.routes.dto.social.toDomain
import com.simbiri.presentation.routes.dto.social.toResponseDto
import java.time.Instant
import java.util.*


fun CommunityUpsertDto.toDomain(
    existingId: String? = null,
    now: Instant = Timestamp.now(),
): Community {
    val ownerUuid = UUID.fromString(ownerId)

    val socialDomain: List<SocialLink> =
        socialLinks.mapNotNull { it.toDomain() }

    return Community(
        id = existingId?.let { CommunityId(UUID.fromString(it)) },
        ownerId = UserId(ownerUuid),
        name = name.trim(),
        description = description.trim(),
        profileUrl = profileUrl,
        memberCount = 0, // derived via membership join table
        joinPermission = JoinPermission.fromCode(joinPermission),
        chatBackgroundUrl = null,
        tagline = tagline.trim(),
        privateEvents = privateEvents,
        privatePosts = privatePosts,
        category = category,
        approved = approved,
        socialLinks = socialDomain,
        createdAt = now,
        updatedAt = now,
    )
}

fun CommunityMemberUpsertDto.toDomain(
    communityId: CommunityId,
    joinedAt: Timestamp = Instant.now(),
): CommunityMember {
    return CommunityMember(
        userId = UserId(UUID.fromString(userId)),
        communityId = communityId,
        joinedAt = joinedAt,
        leftAt = null,
        userTypeAtJoin = null, // you can later populate this from User.userType if you want
        participantRole = ParticipantRole.valueOf(participantRole),
    )
}
fun CommunityMemberUpsertDto.toRole(): ParticipantRole =
    ParticipantRole.valueOf(participantRole.uppercase())

fun Community.toCommResponseDto(): CommunityResponseDto =
    CommunityResponseDto(
        id = id?.value.toString(),
        ownerId = ownerId.value.toString(),
        name = name,
        description = description,
        profileImageUrl = profileUrl,
        chatBackgroundImageUrl = chatBackgroundUrl,
        tagline = tagline,
        memberCount = memberCount,
        privateEvents = privateEvents,
        privatePosts = privatePosts,
        category = category,
        joinPermission = joinPermission.name,
        approved = approved,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
        socialLinks = socialLinks.map { it.toResponseDto() },
    )

fun List<Community>.toCommResponseDto(): List<CommunityResponseDto> =
    this.map { it.toCommResponseDto() }

fun CommunityMember.toMembersResponseDto(): CommunityMemberResponseDto =
    CommunityMemberResponseDto(
        userId = userId.value.toString(),
        communityId = communityId.value.toString(),
        joinedAt = joinedAt.toString(),
        leftAt = leftAt?.toString(),
        userTypeAtJoin = userTypeAtJoin?.code,
        participantRole = participantRole.name,
    )

fun List<CommunityMember>.toMembersResponseDto(): List<CommunityMemberResponseDto> =
    this.map { it.toMembersResponseDto() }
