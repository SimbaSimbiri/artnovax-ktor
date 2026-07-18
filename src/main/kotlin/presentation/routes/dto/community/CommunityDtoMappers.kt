package com.simbiri.presentation.routes.dto.community

import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.community.Community
import com.simbiri.domain.model.community.CommunityMember
import com.simbiri.domain.model.community.CommunityMemberAssignment
import com.simbiri.domain.model.community.CommunityParticipantRole
import com.simbiri.domain.model.community.JoinPermission
import com.simbiri.domain.model.social.SocialLink
import com.simbiri.presentation.routes.dto.social.toDomain
import com.simbiri.presentation.routes.dto.social.toResponseDto

/**
 * Maps a create-request DTO into an unpersisted community.
 */
fun CommunityUpsertDto.toDomainForCreate(
    ownerId: UserId,
    joinPermission: JoinPermission,
): Community =
    toDomainInternal(
        communityId = null,
        ownerId = ownerId,
        joinPermission = joinPermission,
    )

/**
 * Maps an update-request DTO into a domain community.
 */
fun CommunityUpsertDto.toDomainForUpdate(
    communityId: CommunityId,
    ownerId: UserId,
    joinPermission: JoinPermission,
): Community =
    toDomainInternal(
        communityId = communityId,
        ownerId = ownerId,
        joinPermission = joinPermission,
    )

private fun CommunityUpsertDto.toDomainInternal(
    communityId: CommunityId?,
    ownerId: UserId,
    joinPermission: JoinPermission,
): Community {
    val domainSocialLinks: List<SocialLink> =
        socialLinks.mapIndexed { index, socialLinkDto ->
            requireNotNull(socialLinkDto.toDomain()) {
                "Cannot map CommunityUpsertDto to domain because " +
                        "socialLinks[$index] references an unsupported " +
                        "platform. platformId=${socialLinkDto.platformId}, " +
                        "communityId=$communityId, communityName=$name."
            }
        }

    return Community(
        id = communityId,
        ownerId = ownerId,
        name = name.trim(),
        description = description.trim(),
        profileUrl = profileUrl,
        memberCount = 0,
        joinPermission = joinPermission,
        chatBackgroundUrl = chatBackgroundUrl,
        tagline = tagline.trim(),
        privateEvents = privateEvents,
        privatePosts = privatePosts,
        category = category?.trim(),
        approved = approved,
        socialLinks = domainSocialLinks,
        createdAt = null,
        updatedAt = null,
    )
}

/**
 * Maps a member request into a write-intent model.
 *
 * Persistence-managed membership metadata is deliberately absent.
 */
fun CommunityMemberUpsertDto.toAssignment(
    userId: UserId,
    role: CommunityParticipantRole,
): CommunityMemberAssignment =
    CommunityMemberAssignment(
        userId = userId,
        role = role,
    )

/**
 * Maps a persisted community into its API response.
 */
fun Community.toCommResponseDto(): CommunityResponseDto {
    val persistedCommunityId = requireNotNull(id) {
        "Cannot map Community to CommunityResponseDto because " +
                "community.id is null. " +
                "communityName=$name, ownerId=${ownerId.value}."
    }

    val persistedCreatedAt = requireNotNull(createdAt) {
        "Cannot map Community to CommunityResponseDto because " +
                "community.createdAt is null. " +
                "communityId=${persistedCommunityId.value}, " +
                "communityName=$name."
    }

    val persistedUpdatedAt = requireNotNull(updatedAt) {
        "Cannot map Community to CommunityResponseDto because " +
                "community.updatedAt is null. " +
                "communityId=${persistedCommunityId.value}, " +
                "communityName=$name."
    }

    return CommunityResponseDto(
        id = persistedCommunityId.value.toString(),
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
        createdAt = persistedCreatedAt.toString(),
        updatedAt = persistedUpdatedAt.toString(),
        socialLinks = socialLinks.map { socialLink ->
            socialLink.toResponseDto()
        },
    )
}

fun List<Community>.toCommResponseDto(): List<CommunityResponseDto> =
    map { community ->
        community.toCommResponseDto()
    }

fun CommunityMember.toMembersResponseDto(): CommunityMemberResponseDto =
    CommunityMemberResponseDto(
        userId = userId.value.toString(),
        communityId = communityId.value.toString(),
        joinedAt = joinedAt.toString(),
        leftAt = leftAt?.toString(),
        userTypeAtJoin = userTypeAtJoin?.code,
        participantRole = commParticipantRole.name,
    )

fun List<CommunityMember>.toMembersResponseDto():
        List<CommunityMemberResponseDto> =
    map { member ->
        member.toMembersResponseDto()
    }