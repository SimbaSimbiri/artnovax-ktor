package com.simbiri.domain.repository

import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.community.Community
import com.simbiri.domain.model.community.CommunityMember
import com.simbiri.domain.model.community.CommunityMemberAssignment
import com.simbiri.domain.model.community.CommunityParticipantRole
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Defines persistence operations for the Community aggregate and its
 * memberships.
 *
 * The repository accepts typed domain values only. HTTP strings, DTOs,
 * request parsing, and response behavior belong to presentation.
 */
interface CommunityRepository {

    // Community operations

    suspend fun getCommunities(
        approved: Boolean? = null,
        ownerId: UserId? = null,
    ): ResultType<List<Community>, DataError>

    suspend fun getCommunityById(
        communityId: CommunityId,
    ): ResultType<Community, DataError>

    suspend fun createCommunity(
        community: Community,
    ): ResultType<Unit, DataError>

    suspend fun updateCommunity(
        community: Community,
    ): ResultType<Unit, DataError>

    suspend fun createCommunities(
        communities: List<Community>,
    ): ResultType<Unit, DataError>

    suspend fun deleteCommunityById(
        communityId: CommunityId,
    ): ResultType<Unit, DataError>

    // Membership operations

    suspend fun getMembers(
        communityId: CommunityId,
    ): ResultType<List<CommunityMember>, DataError>

    suspend fun addMember(
        communityId: CommunityId,
        userId: UserId,
        role: CommunityParticipantRole,
    ): ResultType<Unit, DataError>

    suspend fun updateMemberRole(
        communityId: CommunityId,
        userId: UserId,
        role: CommunityParticipantRole,
    ): ResultType<Unit, DataError>

    suspend fun addMembers(
        communityId: CommunityId,
        assignments: List<CommunityMemberAssignment>,
    ): ResultType<Unit, DataError>

    suspend fun removeMember(
        communityId: CommunityId,
        userId: UserId,
    ): ResultType<Unit, DataError>
}