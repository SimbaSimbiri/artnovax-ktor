package com.simbiri.domain.repository

import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.community.Community
import com.simbiri.domain.model.community.CommunityMember
import com.simbiri.domain.model.community.CommunityMemberAssignment
import com.simbiri.domain.model.community.CommunityParticipantRole
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

interface CommunityRepository {

    // -----------------------------------------------------------------
    // Target typed community API
    // -----------------------------------------------------------------

    suspend fun getCommunities(
        approved: Boolean? = null,
        ownerId: UserId? = null,
    ): ResultType<List<Community>, DataError> =
        getAllCommunities(
            approved = approved,
            ownerId = ownerId?.value?.toString(),
        )

    suspend fun getCommunityById(
        communityId: CommunityId,
    ): ResultType<Community, DataError> =
        getCommunityById(
            communityId = communityId.value.toString(),
        )

    /**
     * Creates a community.
     *
     * The final persistence implementation must create the community and its
     * OWNER membership in one transaction.
     */
    suspend fun createCommunity(
        community: Community,
    ): ResultType<Unit, DataError> =
        upsertCommunity(community)

    /**
     * Updates an existing community.
     *
     * The final persistence implementation must synchronize ownership when
     * community.ownerId changes.
     */
    suspend fun updateCommunity(
        community: Community,
    ): ResultType<Unit, DataError> =
        upsertCommunity(community)

    suspend fun createCommunities(
        communities: List<Community>,
    ): ResultType<Unit, DataError> =
        insertCommunitiesInBulk(communities)

    suspend fun deleteCommunityById(
        communityId: CommunityId,
    ): ResultType<Unit, DataError> =
        deleteCommunityById(
            communityId = communityId.value.toString(),
        )

    // -----------------------------------------------------------------
    // Target typed membership API
    // -----------------------------------------------------------------

    suspend fun getMembers(
        communityId: CommunityId,
    ): ResultType<List<CommunityMember>, DataError> =
        listMembers(
            communityId = communityId.value.toString(),
        )

    suspend fun addMember(
        communityId: CommunityId,
        userId: UserId,
        role: CommunityParticipantRole,
    ): ResultType<Unit, DataError> =
        upsertMember(
            communityId = communityId.value.toString(),
            userId = userId.value.toString(),
            role = role,
        )

    suspend fun updateMemberRole(
        communityId: CommunityId,
        userId: UserId,
        role: CommunityParticipantRole,
    ): ResultType<Unit, DataError> =
        upsertMember(
            communityId = communityId.value.toString(),
            userId = userId.value.toString(),
            role = role,
        )

    /**
     * Temporary compatibility implementation.
     *
     * CommunityRepoImpl will override this in Phase 2 so the complete batch
     * runs in one transaction.
     */
    suspend fun addMembers(
        communityId: CommunityId,
        assignments: List<CommunityMemberAssignment>,
    ): ResultType<Unit, DataError> {
        for (assignment in assignments) {
            when (
                val result = upsertMember(
                    communityId = communityId.value.toString(),
                    userId = assignment.userId.value.toString(),
                    role = assignment.role,
                )
            ) {
                is ResultType.Success -> Unit
                is ResultType.Failure -> return result
            }
        }

        return ResultType.Success(Unit)
    }

    suspend fun removeMember(
        communityId: CommunityId,
        userId: UserId,
    ): ResultType<Unit, DataError> =
        removeMember(
            communityId = communityId.value.toString(),
            userId = userId.value.toString(),
        )

    // -----------------------------------------------------------------
    // Legacy API
    //
    // Retain temporarily while CommunityRepoImpl and CommunityRoutes are
    // migrated. Remove these methods after Phases 2 and 3.
    // -----------------------------------------------------------------

    @Deprecated(
        message = "Use getCommunities(Boolean?, UserId?) instead.",
    )
    suspend fun getAllCommunities(
        approved: Boolean? = null,
        ownerId: String? = null,
    ): ResultType<List<Community>, DataError>

    @Deprecated(
        message = "Use getCommunityById(CommunityId) instead.",
    )
    suspend fun getCommunityById(
        communityId: String?,
    ): ResultType<Community, DataError>

    @Deprecated(
        message = "Use createCommunity(Community) or updateCommunity(Community).",
    )
    suspend fun upsertCommunity(
        community: Community,
    ): ResultType<Unit, DataError>

    @Deprecated(
        message = "Use deleteCommunityById(CommunityId) instead.",
    )
    suspend fun deleteCommunityById(
        communityId: String?,
    ): ResultType<Unit, DataError>

    @Deprecated(
        message = "Use createCommunities(List<Community>) instead.",
    )
    suspend fun insertCommunitiesInBulk(
        communities: List<Community>,
    ): ResultType<Unit, DataError>

    @Deprecated(
        message = "Use getMembers(CommunityId) instead.",
    )
    suspend fun listMembers(
        communityId: String?,
    ): ResultType<List<CommunityMember>, DataError>

    @Deprecated(
        message = "Use addMember(...) or updateMemberRole(...) instead.",
    )
    suspend fun upsertMember(
        communityId: String?,
        userId: String?,
        role: CommunityParticipantRole,
    ): ResultType<Unit, DataError>

    @Deprecated(
        message = "Use addMembers(CommunityId, assignments) instead.",
    )
    suspend fun upsertMembersInBulk(
        communityId: String?,
        members: List<CommunityMember>,
    ): ResultType<Unit, DataError>

    @Deprecated(
        message = "Use removeMember(CommunityId, UserId) instead.",
    )
    suspend fun removeMember(
        communityId: String?,
        userId: String?,
    ): ResultType<Unit, DataError>
}