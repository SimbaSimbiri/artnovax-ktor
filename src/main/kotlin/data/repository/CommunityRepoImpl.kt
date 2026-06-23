package com.simbiri.data.repository

import com.simbiri.data.database.entity.community.CommunityMemberTable
import com.simbiri.data.database.entity.community.CommunitySocialLinkTable
import com.simbiri.data.database.entity.community.CommunityTable
import com.simbiri.data.database.entity.user.UserTable
import com.simbiri.data.database.utils.dbQuery
import com.simbiri.data.mapper.community.*
import com.simbiri.domain.model.community.Community
import com.simbiri.domain.model.community.CommunityMember
import com.simbiri.domain.model.community.CommunityParticipantRole
import com.simbiri.domain.model.social.SocialLink
import com.simbiri.domain.repository.CommunityRepository
import com.simbiri.data.repository.util.*
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import java.util.UUID

class CommunityRepoImpl(
    private val db: Database,
) : CommunityRepository {

    // -------- Helpers --------

    /**
     * Enforces at most one OWNER per community.
     * - Allows re-assigning OWNER to the same user.
     * - Fails if trying to assign OWNER to a different user when another OWNER exists.
     *
     * Must be called inside dbQuery / transaction.
     */
    private fun ensureOwnerConstraint(
        communityId: UUID,
        userId: UUID,
        newRole: CommunityParticipantRole,
    ): DataError? {
        if (newRole != CommunityParticipantRole.OWNER) return null

        val existingOwnerRow = CommunityMemberTable
            .selectAll()
            .where {
                (CommunityMemberTable.communityId eq communityId) and
                        (CommunityMemberTable.commParticipantRole eq CommunityParticipantRole.OWNER.name)
            }
            .singleOrNull()

        if (existingOwnerRow != null && existingOwnerRow[CommunityMemberTable.userId] != userId) {
            val existingOwnerId = existingOwnerRow[CommunityMemberTable.userId]

            return conflictError(
                operation = "ensureOwnerConstraint",
                message = "Community '$communityId' already has OWNER '$existingOwnerId'. " +
                        "Cannot assign OWNER role to user '$userId'."
            )
        }

        return null
    }

    /**
     * Recalculate and update memberCount for a community.
     * Must be called inside dbQuery / transaction.
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

    private fun communityExistsInternal(communityId: UUID): Boolean =
        CommunityTable
            .selectAll()
            .where { CommunityTable.id eq communityId }
            .limit(1)
            .any()
    
    // -------- Communities CRUD --------

    override suspend fun getAllCommunities(
        approved: Boolean?,
        ownerId: String?,
    ): ResultType<List<Community>, DataError> {
        val operation = "getAllCommunities"

        val ownerUuid: UUID? = if (!ownerId.isNullOrBlank()) {
            when (
                val parsed = parseUuidOrFailure(
                    operation = operation,
                    field = "ownerId",
                    value = ownerId
                )
            ) {
                is ResultType.Success -> parsed.data
                is ResultType.Failure -> return parsed
            }
        } else {
            null
        }

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
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "approved=$approved, ownerId=$ownerId, ownerUuid=$ownerUuid"
                )
            )
        }
    }

    override suspend fun getCommunityById(
        communityId: String?,
    ): ResultType<Community, DataError> {
        val operation = "getCommunityById"

        val uuid = when (
            val parsed = parseUuidOrFailure(
                operation = operation,
                field = "communityId",
                value = communityId
            )
        ) {
            is ResultType.Success -> parsed.data
            is ResultType.Failure -> return parsed
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

            if (community == null) {
                ResultType.Failure(DataError.NotFound)
            } else {
                ResultType.Success(community)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "communityId=$communityId, parsedCommunityUuid=$uuid"
                )
            )
        }
    }

    override suspend fun upsertCommunity(
        community: Community,
    ): ResultType<Unit, DataError> {
        val operation = "upsertCommunity"

        return try {
            db.dbQuery {
                upsertCommunityInternal(community)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "community.id=${community.id}, community.name=${community.name}"
                )
            )
        }
    }

    private fun upsertCommunityInternal(
        community: Community,
    ): ResultType<Unit, DataError> {
        val now = Instant.now()
        val entity = community.toEntity(now)

        if (community.id == null) {
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

        upsertCommunitySocialLinksInternal(
            communityId = entity.id,
            socialLinks = community.socialLinks,
            now = now,
        )

        return ResultType.Success(Unit)
    }

    override suspend fun deleteCommunityById(
        communityId: String?,
    ): ResultType<Unit, DataError> {
        val operation = "deleteCommunityById"

        val uuid = when (
            val parsed = parseUuidOrFailure(
                operation = operation,
                field = "communityId",
                value = communityId
            )
        ) {
            is ResultType.Success -> parsed.data
            is ResultType.Failure -> return parsed
        }

        return try {
            db.dbQuery {
                CommunityMemberTable.deleteWhere { CommunityMemberTable.communityId eq uuid }
                CommunitySocialLinkTable.deleteWhere { CommunitySocialLinkTable.communityId eq uuid }

                val deleted = CommunityTable.deleteWhere { CommunityTable.id eq uuid }

                if (deleted > 0) {
                    ResultType.Success(Unit)
                } else {
                    ResultType.Failure(DataError.NotFound)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "communityId=$communityId, parsedCommunityUuid=$uuid"
                )
            )
        }
    }

    override suspend fun insertCommunitiesInBulk(
        communities: List<Community>,
    ): ResultType<Unit, DataError> {
        val operation = "insertCommunitiesInBulk"

        return try {
            db.dbQuery {
                communities.forEachIndexed { index, community ->
                    when (val res = upsertCommunityInternal(community)) {
                        is ResultType.Failure -> {
                            return@dbQuery ResultType.Failure(
                                DataError.DatabaseError(
                                    operation = operation,
                                    cause = "Bulk community insert failed at index=$index, " +
                                            "community.id=${community.id}, community.name=${community.name}. " +
                                            "Nested error=${res.error}"
                                )
                            )
                        }

                        is ResultType.Success -> Unit
                    }
                }

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "communities.size=${communities.size}"
                )
            )
        }
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

    override suspend fun listMembers(
        communityId: String?,
    ): ResultType<List<CommunityMember>, DataError> {
        val operation = "listMembers"

        val uuid = when (
            val parsed = parseUuidOrFailure(
                operation = operation,
                field = "communityId",
                value = communityId
            )
        ) {
            is ResultType.Success -> parsed.data
            is ResultType.Failure -> return parsed
        }

        return try {
            val membersOrNull = db.dbQuery {
                val exists = communityExistsInternal(uuid)

                if (!exists) {
                    return@dbQuery null
                }

                CommunityMemberTable
                    .selectAll()
                    .where { CommunityMemberTable.communityId eq uuid }
                    .map { it.toCommunityMemberEntity().toDomain() }
            }

            if (membersOrNull == null) {
                ResultType.Failure(DataError.NotFound)
            } else {
                ResultType.Success(membersOrNull)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "communityId=$communityId, parsedCommunityUuid=$uuid"
                )
            )
        }
    }

    override suspend fun upsertMember(
        communityId: String?,
        userId: String?,
        role: CommunityParticipantRole,
    ): ResultType<Unit, DataError> {
        val operation = "upsertMember"

        val communityUuid = when (
            val parsed = parseUuidOrFailure(
                operation = operation,
                field = "communityId",
                value = communityId
            )
        ) {
            is ResultType.Success -> parsed.data
            is ResultType.Failure -> return parsed
        }

        val userUuid = when (
            val parsed = parseUuidOrFailure(
                operation = operation,
                field = "userId",
                value = userId
            )
        ) {
            is ResultType.Success -> parsed.data
            is ResultType.Failure -> return parsed
        }

        return try {
            db.dbQuery {
                val communityExists = communityExistsInternal(communityUuid)
                if (!communityExists) {
                    return@dbQuery ResultType.Failure<DataError>(
                        foreignKeyError(
                            operation = operation,
                            message = "Cannot upsert member because community '$communityUuid' does not exist."
                        )
                    )
                }

                val userRow = UserTable
                    .selectAll()
                    .where { UserTable.id eq userUuid }
                    .singleOrNull()
                    ?: return@dbQuery ResultType.Failure<DataError>(
                        foreignKeyError(
                            operation = operation,
                            message = "Cannot upsert member because user '$userUuid' does not exist."
                        )
                    )

                ensureOwnerConstraint(
                    communityId = communityUuid,
                    userId = userUuid,
                    newRole = role,
                )?.let { error ->
                    return@dbQuery ResultType.Failure<DataError>(error)
                }

                val now = Instant.now()

                val existingMembership = CommunityMemberTable
                    .selectAll()
                    .where {
                        (CommunityMemberTable.communityId eq communityUuid) and
                                (CommunityMemberTable.userId eq userUuid)
                    }
                    .singleOrNull()

                if (existingMembership == null) {
                    val userTypeCode = userRow[UserTable.userType]

                    CommunityMemberTable.insert { row ->
                        row[CommunityMemberTable.communityId] = communityUuid
                        row[CommunityMemberTable.userId] = userUuid
                        row[joinedAt] = now
                        row[leftAt] = null
                        row[userTypeAtJoin] = userTypeCode
                        row[commParticipantRole] = role.name
                    }
                } else {
                    CommunityMemberTable.update({
                        (CommunityMemberTable.communityId eq communityUuid) and
                                (CommunityMemberTable.userId eq userUuid)
                    }) { row ->
                        row[commParticipantRole] = role.name
                    }
                }

                recalcMemberCountInternal(communityUuid)

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "communityId=$communityId, parsedCommunityUuid=$communityUuid, " +
                            "userId=$userId, parsedUserUuid=$userUuid, role=$role"
                )
            )
        }
    }

    override suspend fun upsertMembersInBulk(
        communityId: String?,
        members: List<CommunityMember>,
    ): ResultType<Unit, DataError> {
        val operation = "upsertMembersInBulk"

        val communityUuid = when (
            val parsed = parseUuidOrFailure(
                operation = operation,
                field = "communityId",
                value = communityId
            )
        ) {
            is ResultType.Success -> parsed.data
            is ResultType.Failure -> return parsed
        }

        if (members.isEmpty()) {
            return ResultType.Success(Unit)
        }

        return try {
            db.dbQuery {
                val communityExists = communityExistsInternal(communityUuid)
                if (!communityExists) {
                    return@dbQuery ResultType.Failure<DataError>(
                        foreignKeyError(
                            operation = operation,
                            message = "Cannot bulk upsert members because community '$communityUuid' does not exist."
                        )
                    )
                }

                val ownersInPayload = members.count {
                    it.commParticipantRole == CommunityParticipantRole.OWNER
                }

                if (ownersInPayload > 1) {
                    return@dbQuery ResultType.Failure<DataError>(
                        validationError(
                            operation = operation,
                            field = "members",
                            value = "ownersInPayload=$ownersInPayload",
                            reason = "Bulk payload cannot contain more than one OWNER."
                        )
                    )
                }

                members.forEachIndexed { index, member ->
                    val userUuid = member.userId.value
                    val memberRole = member.commParticipantRole

                    val userRow = UserTable
                        .selectAll()
                        .where { UserTable.id eq userUuid }
                        .singleOrNull()
                        ?: return@dbQuery ResultType.Failure<DataError>(
                            foreignKeyError(
                                operation = operation,
                                message = "Cannot insert member at index=$index because user '$userUuid' does not exist."
                            )
                        )

                    ensureOwnerConstraint(
                        communityId = communityUuid,
                        userId = userUuid,
                        newRole = memberRole,
                    )?.let { error ->
                        return@dbQuery ResultType.Failure<DataError>(
                            DataError.Conflict(
                                message = "Bulk member upsert failed at index=$index for user '$userUuid'. " +
                                        "Nested error=$error"
                            )
                        )
                    }

                    CommunityMemberTable.deleteWhere {
                        (CommunityMemberTable.communityId eq communityUuid) and
                                (CommunityMemberTable.userId eq userUuid)
                    }

                    val userTypeCode = member.userTypeAtJoin?.code ?: userRow[UserTable.userType]

                    CommunityMemberTable.insert { row ->
                        row[CommunityMemberTable.communityId] = communityUuid
                        row[CommunityMemberTable.userId] = userUuid
                        row[CommunityMemberTable.joinedAt] = member.joinedAt
                        row[CommunityMemberTable.leftAt] = member.leftAt
                        row[CommunityMemberTable.userTypeAtJoin] = userTypeCode
                        row[CommunityMemberTable.commParticipantRole] = memberRole.name
                    }
                }

                recalcMemberCountInternal(communityUuid)

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "communityId=$communityId, parsedCommunityUuid=$communityUuid, members.size=${members.size}"
                )
            )
        }
    }

    override suspend fun removeMember(
        communityId: String?,
        userId: String?,
    ): ResultType<Unit, DataError> {
        val operation = "removeMember"

        val communityUuid = when (
            val parsed = parseUuidOrFailure(
                operation = operation,
                field = "communityId",
                value = communityId
            )
        ) {
            is ResultType.Success -> parsed.data
            is ResultType.Failure -> return parsed
        }

        val userUuid = when (
            val parsed = parseUuidOrFailure(
                operation = operation,
                field = "userId",
                value = userId
            )
        ) {
            is ResultType.Success -> parsed.data
            is ResultType.Failure -> return parsed
        }

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
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "communityId=$communityId, parsedCommunityUuid=$communityUuid, " +
                            "userId=$userId, parsedUserUuid=$userUuid"
                )
            )
        }
    }
}