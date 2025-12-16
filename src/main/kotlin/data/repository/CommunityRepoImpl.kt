package com.simbiri.data.repository

import com.simbiri.data.database.entity.community.CommunityMemberTable
import com.simbiri.data.database.entity.community.CommunitySocialLinkTable
import com.simbiri.data.database.entity.community.CommunityTable
import com.simbiri.data.database.entity.user.UserTable
import com.simbiri.data.database.utils.dbQuery
import com.simbiri.data.mapper.community.*
import com.simbiri.domain.model.community.Community
import com.simbiri.domain.model.community.CommunityMember
import com.simbiri.domain.model.community.ParticipantRole
import com.simbiri.domain.model.social.SocialLink
import com.simbiri.domain.repository.CommunityRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import java.util.*

class CommunityRepoImpl(
    private val db: Database,
) : CommunityRepository {

    // -------- Helpers --------

    /**
     * Enforces at most one OWNER per community.
     * - Allows re-assigning OWNER to the same user (idempotent).
     * - Fails if trying to assign OWNER to a *different* user when another OWNER exists.
     */
    private fun ensureOwnerConstraint(
        communityId: UUID,
        userId: UUID,
        newRole: ParticipantRole,
    ): DataError? {
        if (newRole != ParticipantRole.OWNER) return null

        val existingOwnerRow = CommunityMemberTable
            .selectAll()
            .where {
                (CommunityMemberTable.communityId eq communityId) and
                        (CommunityMemberTable.participantRole eq ParticipantRole.OWNER.name)
            }
            .singleOrNull()

        if (existingOwnerRow != null && existingOwnerRow[CommunityMemberTable.userId] != userId) {
            return DataError.ValidationError
        }
        return null
    }

    /**
     * Recalculate and update memberCount for a community.
     * Must be called inside an Exposed transaction (i.e., inside dbQuery).
     */
    private fun recalcMemberCountInternal(communityId: UUID) {
        val memberCount = CommunityMemberTable
            .selectAll()
            .where { CommunityMemberTable.communityId eq communityId }
            .count()
            .toInt()

        CommunityTable.update({ CommunityTable.id eq communityId }) {
            it[CommunityTable.memberCount] = memberCount
        }
    }

    // -------- Communities CRUD --------

    override suspend fun getAllCommunities(
        approved: Boolean?,
        ownerId: String?,
    ): ResultType<List<Community>, DataError> {
        // Parse ownerId up front so an invalid UUID becomes a clear validation error
        val ownerUuid: UUID? = if (!ownerId.isNullOrBlank()) {
            runCatching { UUID.fromString(ownerId) }.getOrElse {
                return ResultType.Failure(DataError.ValidationError)
            }
        } else null

        return try {
            val communities = db.dbQuery {
                var query: Query = CommunityTable.selectAll()

                if (approved != null) {
                    query = query.andWhere { CommunityTable.approved eq approved }
                }

                if (ownerUuid != null) {
                    query = query.andWhere { CommunityTable.ownerId eq ownerUuid }
                }

                val rows = query.toList()
                if (rows.isEmpty()) return@dbQuery emptyList()

                val entities = rows.map { it.toCommunityEntity() }
                val communityIds = entities.map { it.id }.toSet()

                val socialRows = CommunitySocialLinkTable
                    .selectAll()
                    .where { CommunitySocialLinkTable.communityId inList communityIds }
                    .toList()

                val socialByCommunityId = socialRows
                    .groupBy { it[CommunitySocialLinkTable.communityId] }
                    .mapValues { (_, rs) ->
                        rs.map { row -> row.toCommunitySocialLinkEntity().toDomain() }
                    }

                entities.map { entity ->
                    val socials = socialByCommunityId[entity.id].orEmpty()
                    entity.toDomain(socialLinks = socials)
                }
            }

            if (communities.isEmpty()) {
                ResultType.Failure(DataError.NotFound)
            } else {
                ResultType.Success(communities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(DataError.DatabaseError)
        }
    }

    override suspend fun getCommunityById(communityId: String?): ResultType<Community, DataError> {
        if (communityId.isNullOrBlank()) {
            return ResultType.Failure(DataError.ValidationError)
        }

        val uuid = try {
            UUID.fromString(communityId)
        } catch (_: IllegalArgumentException) {
            return ResultType.Failure(DataError.ValidationError)
        }

        return try {
            val community = db.dbQuery {
                val row = CommunityTable
                    .selectAll()
                    .where { CommunityTable.id eq uuid }
                    .singleOrNull()
                    ?: return@dbQuery null

                val entity = row.toCommunityEntity()

                val socialRows = CommunitySocialLinkTable
                    .selectAll()
                    .where { CommunitySocialLinkTable.communityId eq uuid }
                    .toList()

                val socials = socialRows.map { it.toCommunitySocialLinkEntity().toDomain() }

                entity.toDomain(socialLinks = socials)
            }

            if (community == null) ResultType.Failure(DataError.NotFound)
            else ResultType.Success(community)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(DataError.DatabaseError)
        }
    }

    override suspend fun upsertCommunity(community: Community): ResultType<Unit, DataError> =
        try {
            db.dbQuery {
                upsertCommunityInternal(community)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(DataError.DatabaseError)
        }

    private fun upsertCommunityInternal(community: Community): ResultType<Unit, DataError> {
        val now = Instant.now()
        val entity = community.toEntity(now)

        if (community.id == null) {
            // INSERT
            CommunityTable.insert { row ->
                row[CommunityTable.id] = entity.id
                row[ownerId] = entity.ownerId
                row[name] = entity.name
                row[description] = entity.description
                row[profileUrl] = entity.profileUrl
                row[memberCount] = 0
                row[joinPermission] = entity.joinPermissionCode
                row[chatBackgroundUrl] = entity.chatBackgroundUrl
                row[tagline] = entity.tagline
                row[privateEvents] = entity.privateEvents
                row[privatePosts] = entity.privatePosts
                row[category] = entity.category
                row[approved] = entity.approved
                row[createdAt] = entity.createdAt
                row[updatedAt] = entity.updatedAt
            }
        } else {
            // UPDATE (do NOT touch memberCount; derived from membership)
            val updated = CommunityTable.update(
                where = { CommunityTable.id eq entity.id },
            ) { row ->
                row[ownerId] = entity.ownerId
                row[name] = entity.name
                row[description] = entity.description
                row[profileUrl] = entity.profileUrl
                row[joinPermission] = entity.joinPermissionCode
                row[chatBackgroundUrl] = entity.chatBackgroundUrl
                row[tagline] = entity.tagline
                row[privateEvents] = entity.privateEvents
                row[privatePosts] = entity.privatePosts
                row[category] = entity.category
                row[approved] = entity.approved
                row[updatedAt] = entity.updatedAt
            }

            if (updated == 0) {
                return ResultType.Failure(DataError.NotFound)
            }
        }

        // Replace community social links
        upsertCommunitySocialLinksInternal(
            communityId = entity.id,
            socialLinks = community.socialLinks,
            now = now,
        )

        return ResultType.Success(Unit)
    }

    override suspend fun deleteCommunityById(communityId: String?): ResultType<Unit, DataError> {
        if (communityId.isNullOrBlank()) {
            return ResultType.Failure(DataError.ValidationError)
        }

        val uuid = try {
            UUID.fromString(communityId)
        } catch (_: IllegalArgumentException) {
            return ResultType.Failure(DataError.ValidationError)
        }

        return try {
            db.dbQuery {
                CommunityMemberTable.deleteWhere { CommunityMemberTable.communityId eq uuid }
                CommunitySocialLinkTable.deleteWhere { CommunitySocialLinkTable.communityId eq uuid }

                val deleted = CommunityTable.deleteWhere { CommunityTable.id eq uuid }
                if (deleted > 0) ResultType.Success(Unit)
                else ResultType.Failure(DataError.NotFound)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(DataError.DatabaseError)
        }
    }

    override suspend fun insertCommunitiesInBulk(
        communities: List<Community>,
    ): ResultType<Unit, DataError> =
        try {
            db.dbQuery {
                for (community in communities) {
                    when (val res = upsertCommunityInternal(community)) {
                        is ResultType.Failure -> return@dbQuery res
                        is ResultType.Success -> Unit
                    }
                }
                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(DataError.DatabaseError)
        }

    private fun upsertCommunitySocialLinksInternal(
        communityId: UUID,
        socialLinks: List<SocialLink>,
        now: Instant,
    ) {
        CommunitySocialLinkTable.deleteWhere {
            CommunitySocialLinkTable.communityId eq communityId
        }

        socialLinks.forEach { link ->
            val entity = link.toCommunitySocialLinkEntity(
                communityId = communityId,
                createdAt = now,
            )

            CommunitySocialLinkTable.insert { row ->
                row[CommunitySocialLinkTable.id] = entity.id
                row[CommunitySocialLinkTable.communityId] = entity.communityId
                row[platformId] = entity.platformId
                row[username] = entity.username
                row[completeUrl] = entity.completeUrl
                row[createdAt] = entity.createdAt
            }
        }
    }

    // -------- Membership --------

    override suspend fun listMembers(communityId: String?): ResultType<List<CommunityMember>, DataError> {
        if (communityId.isNullOrBlank()) {
            return ResultType.Failure(DataError.ValidationError)
        }

        val uuid = try {
            UUID.fromString(communityId)
        } catch (_: IllegalArgumentException) {
            return ResultType.Failure(DataError.ValidationError)
        }

        return try {
            val membersOrNull = db.dbQuery {
                // Distinguish "community doesn't exist" from "no members yet"
                val exists = CommunityTable
                    .selectAll()
                    .where { CommunityTable.id eq uuid }
                    .limit(1)
                    .any()

                if (!exists) {
                    return@dbQuery null
                }

                CommunityMemberTable
                    .selectAll()
                    .where { CommunityMemberTable.communityId eq uuid }
                    .map { it.toCommunityMemberEntity().toDomain() }
            }

            when {
                membersOrNull == null -> ResultType.Failure(DataError.NotFound)
                else -> ResultType.Success(membersOrNull) // possibly empty, which is fine
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(DataError.DatabaseError)
        }
    }

    override suspend fun upsertMember(
        communityId: String?,
        userId: String?,
        role: ParticipantRole,
    ): ResultType<Unit, DataError> {
        if (communityId.isNullOrBlank() || userId.isNullOrBlank()) {
            return ResultType.Failure(DataError.ValidationError)
        }

        val communityUuid = runCatching { UUID.fromString(communityId) }.getOrNull()
            ?: return ResultType.Failure(DataError.ValidationError)
        val userUuid = runCatching { UUID.fromString(userId) }.getOrNull()
            ?: return ResultType.Failure(DataError.ValidationError)

        return try {
            db.dbQuery {
                // Enforce single OWNER
                ensureOwnerConstraint(
                    communityId = communityUuid,
                    userId = userUuid,
                    newRole = role,
                )?.let { error ->
                    return@dbQuery ResultType.Failure<DataError>(error)
                }

                val now = Instant.now()

                // Check if membership already exists
                val existingMembership = CommunityMemberTable
                    .selectAll()
                    .where {
                        (CommunityMemberTable.communityId eq communityUuid) and
                                (CommunityMemberTable.userId eq userUuid)
                    }
                    .singleOrNull()

                if (existingMembership == null) {
                    // New membership → snapshot current global UserType
                    val userTypeCode = UserTable
                        .selectAll()
                        .where { UserTable.id eq userUuid }
                        .singleOrNull()
                        ?.get(UserTable.userType)
                        ?: return@dbQuery ResultType.Failure<DataError>(DataError.NotFound)

                    CommunityMemberTable.insert { row ->
                        row[CommunityMemberTable.communityId] = communityUuid
                        row[CommunityMemberTable.userId] = userUuid
                        row[joinedAt] = now
                        row[leftAt] = null
                        row[userTypeAtJoin] = userTypeCode
                        row[participantRole] = role.name
                    }
                } else {
                    // Existing membership → only update role, keep joinedAt & userTypeAtJoin
                    CommunityMemberTable.update({
                        (CommunityMemberTable.communityId eq communityUuid) and
                                (CommunityMemberTable.userId eq userUuid)
                    }) { row ->
                        row[participantRole] = role.name
                    }
                }

                recalcMemberCountInternal(communityUuid)

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(DataError.DatabaseError)
        }
    }

    override suspend fun upsertMembersInBulk(
        communityId: String?,
        members: List<CommunityMember>,
    ): ResultType<Unit, DataError> {
        if (communityId.isNullOrBlank()) {
            return ResultType.Failure(DataError.ValidationError)
        }
        if (members.isEmpty()) {
            return ResultType.Success(Unit) // nothing to do
        }

        val communityUuid = runCatching { UUID.fromString(communityId) }.getOrNull()
            ?: return ResultType.Failure(DataError.ValidationError)

        return try {
            db.dbQuery {
                // 1) Check community exists
                val exists = CommunityTable
                    .selectAll()
                    .where { CommunityTable.id eq communityUuid }
                    .limit(1)
                    .any()

                if (!exists) {
                    return@dbQuery ResultType.Failure<DataError>(DataError.NotFound)
                }

                // 2) Enforce at most one OWNER in the payload itself
                val ownersInPayload = members.count { it.participantRole == ParticipantRole.OWNER }
                if (ownersInPayload > 1) {
                    return@dbQuery ResultType.Failure<DataError>(DataError.ValidationError)
                }

                // 3) Upsert each member
                members.forEach { member ->
                    val userUuid = member.userId.value  // adjust if your UserId type differs

                    // enforce user exists
                    val userRow = UserTable
                        .selectAll()
                        .where { UserTable.id eq userUuid }
                        .singleOrNull()
                        ?: return@dbQuery ResultType.Failure<DataError>(DataError.NotFound)

                    // Enforce single OWNER (community-wide)
                    ensureOwnerConstraint(
                        communityId = communityUuid,
                        userId = userUuid,
                        newRole = member.participantRole,
                    )?.let { error ->
                        return@dbQuery ResultType.Failure<DataError>(error)
                    }

                    // delete any existing membership row, then insert fresh
                    CommunityMemberTable.deleteWhere {
                        (CommunityMemberTable.communityId eq communityUuid) and
                                (CommunityMemberTable.userId eq userUuid)
                    }

                    val userTypeCode = member.userTypeAtJoin?.code ?: userRow[UserTable.userType]
                    val joinedAt = member.joinedAt
                    val leftAt = member.leftAt

                    CommunityMemberTable.insert { row ->
                        row[CommunityMemberTable.communityId] = communityUuid
                        row[CommunityMemberTable.userId] = userUuid
                        row[CommunityMemberTable.joinedAt] = joinedAt
                        row[CommunityMemberTable.leftAt] = leftAt
                        row[CommunityMemberTable.userTypeAtJoin] = userTypeCode
                        row[CommunityMemberTable.participantRole] = member.participantRole.name
                    }
                }

                recalcMemberCountInternal(communityUuid)

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(DataError.DatabaseError)
        }
    }

    override suspend fun removeMember(
        communityId: String?,
        userId: String?,
    ): ResultType<Unit, DataError> {
        if (communityId.isNullOrBlank() || userId.isNullOrBlank()) {
            return ResultType.Failure(DataError.ValidationError)
        }

        val communityUuid = runCatching { UUID.fromString(communityId) }.getOrNull()
            ?: return ResultType.Failure(DataError.ValidationError)
        val userUuid = runCatching { UUID.fromString(userId) }.getOrNull()
            ?: return ResultType.Failure(DataError.ValidationError)

        return try {
            db.dbQuery {
                val deleted = CommunityMemberTable.deleteWhere {
                    (CommunityMemberTable.communityId eq communityUuid) and
                            (CommunityMemberTable.userId eq userUuid)
                }

                if (deleted > 0) {
                    recalcMemberCountInternal(communityUuid)
                    ResultType.Success(Unit)
                } else {
                    ResultType.Failure(DataError.NotFound)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(DataError.DatabaseError)
        }
    }
}
