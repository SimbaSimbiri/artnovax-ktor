package com.simbiri.domain.repository

import com.simbiri.domain.model.community.Community
import com.simbiri.domain.model.community.CommunityMember
import com.simbiri.domain.model.community.ParticipantRole
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

interface CommunityRepository {
    // CRUD Communities
    suspend fun getAllCommunities(approved: Boolean? = null, ownerId: String? = null): ResultType<List<Community>, DataError>
    suspend fun getCommunityById(communityId: String?): ResultType<Community, DataError>
    suspend fun upsertCommunity(community: Community): ResultType<Unit, DataError>
    suspend fun deleteCommunityById(communityId: String?): ResultType<Unit, DataError>
    suspend fun insertCommunitiesInBulk(communities: List<Community>): ResultType<Unit, DataError>

    // CRUD Membership
    suspend fun listMembers(communityId: String?): ResultType<List<CommunityMember>, DataError>
    suspend fun upsertMember(communityId: String?, userId: String?, role: ParticipantRole): ResultType<Unit, DataError>

    suspend fun upsertMembersInBulk(communityId: String?, members: List<CommunityMember>): ResultType<Unit, DataError>
    suspend fun removeMember(communityId: String?, userId: String?): ResultType<Unit, DataError>

}