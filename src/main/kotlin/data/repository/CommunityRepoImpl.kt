package com.simbiri.data.repository

import com.simbiri.data.database.entity.community.CommunityMemberTable
import com.simbiri.data.database.entity.community.CommunitySocialLinkTable
import com.simbiri.data.database.entity.community.CommunityTable
import com.simbiri.data.database.entity.social.SocialPlatformTable
import com.simbiri.data.database.entity.user.UserTable
import com.simbiri.data.database.utils.dbQuery
import com.simbiri.data.mapper.community.toCommunityEntity
import com.simbiri.data.mapper.community.toCommunityMemberEntity
import com.simbiri.data.mapper.community.toCommunitySocialLinkEntity
import com.simbiri.data.mapper.community.toDomain
import com.simbiri.data.mapper.community.toEntity
import com.simbiri.data.repository.util.conflictError
import com.simbiri.data.repository.util.databaseError
import com.simbiri.data.repository.util.duplicateResourceError
import com.simbiri.data.repository.util.foreignKeyError
import com.simbiri.data.repository.util.parseUuidOrFailure
import com.simbiri.data.repository.util.validationError
import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.community.Community
import com.simbiri.domain.model.community.CommunityMember
import com.simbiri.domain.model.community.CommunityMemberAssignment
import com.simbiri.domain.model.community.CommunityParticipantRole
import com.simbiri.domain.model.social.SocialLink
import com.simbiri.domain.repository.CommunityRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class CommunityRepoImpl(
    private val db: Database,
) : CommunityRepository {

    // -----------------------------------------------------------------
    // Error context
    // -----------------------------------------------------------------

    private fun withBulkCommunityContext(
        error: DataError,
        operation: String,
        index: Int,
        community: Community,
    ): DataError {
        val context =
            "Bulk community creation failed in $operation at index=$index. " +
                    "community.id=${community.id}, " +
                    "community.name=${community.name}, " +
                    "ownerId=${community.ownerId.value}."

        return addContextToError(
            error = error,
            operation = operation,
            context = context,
        )
    }

    private fun withBulkMemberContext(
        error: DataError,
        operation: String,
        index: Int,
        assignment: CommunityMemberAssignment,
        communityId: CommunityId,
    ): DataError {
        val context =
            "Bulk community-member creation failed in $operation " +
                    "at index=$index. " +
                    "communityId=${communityId.value}, " +
                    "userId=${assignment.userId.value}, " +
                    "role=${assignment.role}."

        return addContextToError(
            error = error,
            operation = operation,
            context = context,
        )
    }

    private fun addContextToError(
        error: DataError,
        operation: String,
        context: String,
    ): DataError =
        when (error) {
            DataError.NotFound ->
                DataError.NotFound

            is DataError.ValidationError ->
                DataError.ValidationError(
                    "$context Nested validation error: ${error.message}"
                )

            is DataError.DatabaseError ->
                DataError.DatabaseError(
                    operation = operation,
                    cause = "$context Nested database error from " +
                            "${error.operation}: ${error.cause}"
                )

            is DataError.ForeignKeyViolation ->
                DataError.ForeignKeyViolation(
                    "$context Nested foreign-key error: ${error.message}"
                )

            is DataError.Conflict ->
                DataError.Conflict(
                    "$context Nested conflict error: ${error.message}"
                )

            is DataError.DuplicateResource ->
                DataError.DuplicateResource(
                    "$context Nested duplicate-resource error: ${error.message}"
                )

            is DataError.UnknownError ->
                DataError.UnknownError(
                    "$context Nested unknown error: ${error.cause}"
                )
        }

    // -----------------------------------------------------------------
    // Database lookup helpers
    //
    // All helpers here must be called inside dbQuery.
    // -----------------------------------------------------------------

    private fun communityExistsInternal(
        communityId: UUID,
    ): Boolean =
        CommunityTable
            .selectAll()
            .where {
                CommunityTable.id eq communityId
            }
            .limit(1)
            .any()

    private fun userExistsInternal(
        userId: UUID,
    ): Boolean =
        UserTable
            .selectAll()
            .where {
                UserTable.id eq userId
            }
            .limit(1)
            .any()

    private fun communityMemberExistsInternal(
        communityId: UUID,
        userId: UUID,
    ): Boolean =
        CommunityMemberTable
            .selectAll()
            .where {
                (CommunityMemberTable.communityId eq communityId) and
                        (CommunityMemberTable.userId eq userId)
            }
            .limit(1)
            .any()

    private fun socialPlatformExistsInternal(
        platformId: Int,
    ): Boolean =
        SocialPlatformTable
            .selectAll()
            .where {
                SocialPlatformTable.id eq EntityID(
                    id = platformId,
                    table = SocialPlatformTable,
                )
            }
            .limit(1)
            .any()

    private fun loadCommunityOwnerIdInternal(
        communityId: UUID,
    ): UUID? =
        CommunityTable
            .selectAll()
            .where {
                CommunityTable.id eq communityId
            }
            .singleOrNull()
            ?.get(CommunityTable.ownerId)

    private fun loadUserTypeCodeInternal(
        userId: UUID,
    ): Int? =
        UserTable
            .selectAll()
            .where {
                UserTable.id eq userId
            }
            .singleOrNull()
            ?.get(UserTable.userType)

    private fun loadUserTypeCodesInternal(
        userIds: Set<UUID>,
    ): Map<UUID, Int> {
        if (userIds.isEmpty()) {
            return emptyMap()
        }

        val userEntityIds = userIds.map { userId ->
            EntityID(
                id = userId,
                table = UserTable,
            )
        }

        return UserTable
            .selectAll()
            .where {
                UserTable.id inList userEntityIds
            }
            .associate { row ->
                row[UserTable.id].value to row[UserTable.userType]
            }
    }

    // -----------------------------------------------------------------
    // Persistence validation
    // -----------------------------------------------------------------

    private fun validateCommunityReferencesInternal(
        operation: String,
        community: Community,
    ): DataError? {
        val ownerUuid = community.ownerId.value

        if (!userExistsInternal(ownerUuid)) {
            return foreignKeyError(
                operation = operation,
                message = "Community owner does not exist. " +
                        "ownerId=$ownerUuid, community.name=${community.name}."
            )
        }

        community.socialLinks.forEachIndexed { index, socialLink ->
            val platformId = socialLink.platform.id

            if (!socialPlatformExistsInternal(platformId)) {
                return foreignKeyError(
                    operation = operation,
                    message = "community.socialLinks[$index] references " +
                            "socialPlatformId=$platformId, but no matching " +
                            "row exists in SocialPlatformTable."
                )
            }
        }

        return null
    }

    private fun validateMemberRoleAgainstOwnerInternal(
        operation: String,
        communityId: UUID,
        communityOwnerId: UUID,
        userId: UUID,
        requestedRole: CommunityParticipantRole,
    ): DataError? {
        if (
            requestedRole == CommunityParticipantRole.OWNER &&
            userId != communityOwnerId
        ) {
            return conflictError(
                operation = operation,
                message = "Cannot assign OWNER membership to user '$userId' " +
                        "because CommunityTable identifies '$communityOwnerId' " +
                        "as the owner of community '$communityId'. " +
                        "Change community.ownerId through updateCommunity first."
            )
        }

        if (
            requestedRole != CommunityParticipantRole.OWNER &&
            userId == communityOwnerId
        ) {
            return conflictError(
                operation = operation,
                message = "Cannot assign role '$requestedRole' to user " +
                        "'$userId' because that user is the recorded owner " +
                        "of community '$communityId'."
            )
        }

        return null
    }

    private fun validateBulkAssignmentsShapeInternal(
        operation: String,
        assignments: List<CommunityMemberAssignment>,
    ): DataError? {
        val duplicateUser =
            assignments
                .groupingBy { assignment ->
                    assignment.userId
                }
                .eachCount()
                .entries
                .firstOrNull { (_, count) ->
                    count > 1
                }

        if (duplicateUser != null) {
            return validationError(
                operation = operation,
                field = "assignments",
                value = duplicateUser.key.value.toString(),
                reason = "Bulk membership payload cannot contain the same " +
                        "user more than once."
            )
        }

        val ownerCount = assignments.count { assignment ->
            assignment.role == CommunityParticipantRole.OWNER
        }

        if (ownerCount > 1) {
            return validationError(
                operation = operation,
                field = "assignments",
                value = "ownerCount=$ownerCount",
                reason = "Bulk membership payload cannot contain more than " +
                        "one OWNER assignment."
            )
        }

        return null
    }

    // -----------------------------------------------------------------
    // Typed community API
    // -----------------------------------------------------------------

    override suspend fun getCommunities(
        approved: Boolean?,
        ownerId: UserId?,
    ): ResultType<List<Community>, DataError> {
        val operation = "getCommunities"

        return try {
            val communities = db.dbQuery {
                var query: Query = CommunityTable.selectAll()

                if (approved != null) {
                    query = query.andWhere {
                        CommunityTable.approved eq approved
                    }
                }

                if (ownerId != null) {
                    query = query.andWhere {
                        CommunityTable.ownerId eq ownerId.value
                    }
                }

                val entities = query
                    .toList()
                    .map(ResultRow::toCommunityEntity)

                if (entities.isEmpty()) {
                    return@dbQuery emptyList<Community>()
                }

                val socialLinksByCommunityId =
                    loadCommunitySocialLinksInternal(
                        communityIds = entities
                            .map { entity -> entity.id }
                            .toSet(),
                    )

                entities.map { entity ->
                    entity.toDomain(
                        socialLinks =
                            socialLinksByCommunityId[entity.id].orEmpty()
                    )
                }
            }

            if (communities.isEmpty()) {
                ResultType.Failure(DataError.NotFound)
            } else {
                ResultType.Success(communities)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "approved=$approved, " +
                            "ownerId=${ownerId?.value}"
                )
            )
        }
    }

    override suspend fun getCommunityById(
        communityId: CommunityId,
    ): ResultType<Community, DataError> {
        val operation = "getCommunityById"
        val uuid = communityId.value

        return try {
            val community = db.dbQuery {
                loadCommunityByIdInternal(uuid)
            }

            if (community == null) {
                ResultType.Failure(DataError.NotFound)
            } else {
                ResultType.Success(community)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "communityId=$uuid"
                )
            )
        }
    }

    override suspend fun createCommunity(
        community: Community,
    ): ResultType<Unit, DataError> {
        val operation = "createCommunity"

        if (community.id != null) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "community.id",
                    value = community.id.value.toString(),
                    reason = "A new community must not already have an ID."
                )
            )
        }

        return try {
            db.dbQuery {
                validateCommunityReferencesInternal(
                    operation = operation,
                    community = community,
                )?.let { error ->
                    return@dbQuery ResultType.Failure(error)
                }

                insertCommunityInternal(
                    community = community,
                    now = Instant.now(),
                )

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = communityWriteDetails(community)
                )
            )
        }
    }

    override suspend fun updateCommunity(
        community: Community,
    ): ResultType<Unit, DataError> {
        val operation = "updateCommunity"

        val communityUuid = community.id?.value
            ?: return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "community.id",
                    value = null,
                    reason = "An existing community ID is required for update."
                )
            )

        return try {
            db.dbQuery {
                if (!communityExistsInternal(communityUuid)) {
                    return@dbQuery ResultType.Failure(
                        DataError.NotFound
                    )
                }

                validateCommunityReferencesInternal(
                    operation = operation,
                    community = community,
                )?.let { error ->
                    return@dbQuery ResultType.Failure(error)
                }

                val now = Instant.now()

                updateCommunityRowInternal(
                    community = community,
                    now = now
                )

                replaceCommunitySocialLinksInternal(
                    communityId = communityUuid,
                    socialLinks = community.socialLinks,
                    now = now
                )

                synchronizeOwnerMembershipInternal(
                    communityId = communityUuid,
                    ownerId = community.ownerId.value,
                    now = now
                )

                recalculateMemberCountInternal(communityUuid)

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = communityWriteDetails(community)
                )
            )
        }
    }

    override suspend fun createCommunities(
        communities: List<Community>,
    ): ResultType<Unit, DataError> {
        val operation = "createCommunities"

        if (communities.isEmpty()) {
            return ResultType.Success(Unit)
        }

        return try {
            db.dbQuery {
                /*
                 * Validate the full batch before the first insert so returning
                 * a Failure cannot commit an earlier partial batch.
                 */
                communities.forEachIndexed { index, community ->
                    if (community.id != null) {
                        return@dbQuery ResultType.Failure(
                            withBulkCommunityContext(
                                error = validationError(
                                    operation = operation,
                                    field = "communities[$index].id",
                                    value = community.id.value.toString(),
                                    reason = "Bulk creation only accepts " +
                                            "communities without existing IDs."
                                ),
                                operation = operation,
                                index = index,
                                community = community,
                            )
                        )
                    }

                    validateCommunityReferencesInternal(
                        operation = operation,
                        community = community,
                    )?.let { error ->
                        return@dbQuery ResultType.Failure(
                            withBulkCommunityContext(
                                error = error,
                                operation = operation,
                                index = index,
                                community = community,
                            )
                        )
                    }
                }

                val now = Instant.now()

                communities.forEach { community ->
                    insertCommunityInternal(
                        community = community,
                        now = now,
                    )
                }

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "communities.size=${communities.size}, " +
                            "names=${communities.map { it.name }}, " +
                            "ownerIds=${communities.map { it.ownerId.value }}"
                )
            )
        }
    }

    override suspend fun deleteCommunityById(
        communityId: CommunityId,
    ): ResultType<Unit, DataError> {
        val operation = "deleteCommunityById"
        val uuid = communityId.value

        return try {
            db.dbQuery {
                if (!communityExistsInternal(uuid)) {
                    return@dbQuery ResultType.Failure(
                        DataError.NotFound
                    )
                }

                CommunityMemberTable.deleteWhere {
                    CommunityMemberTable.communityId eq uuid
                }

                CommunitySocialLinkTable.deleteWhere {
                    CommunitySocialLinkTable.communityId eq uuid
                }

                val deletedCount = CommunityTable.deleteWhere {
                    CommunityTable.id eq uuid
                }

                if (deletedCount == 0) {
                    ResultType.Failure(DataError.NotFound)
                } else {
                    ResultType.Success(Unit)
                }
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "communityId=$uuid"
                )
            )
        }
    }

    // -----------------------------------------------------------------
    // Typed membership API
    // -----------------------------------------------------------------

    override suspend fun getMembers(
        communityId: CommunityId,
    ): ResultType<List<CommunityMember>, DataError> {
        val operation = "getMembers"
        val communityUuid = communityId.value

        return try {
            val members = db.dbQuery {
                if (!communityExistsInternal(communityUuid)) {
                    return@dbQuery null
                }

                CommunityMemberTable
                    .selectAll()
                    .where {
                        CommunityMemberTable.communityId eq communityUuid
                    }
                    .map { row ->
                        row
                            .toCommunityMemberEntity()
                            .toDomain()
                    }
            }

            if (members == null) {
                ResultType.Failure(DataError.NotFound)
            } else {
                ResultType.Success(members)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "communityId=$communityUuid"
                )
            )
        }
    }

    override suspend fun addMember(
        communityId: CommunityId,
        userId: UserId,
        role: CommunityParticipantRole,
    ): ResultType<Unit, DataError> {
        val operation = "addMember"
        val communityUuid = communityId.value
        val userUuid = userId.value

        return try {
            db.dbQuery {
                val ownerUuid =
                    loadCommunityOwnerIdInternal(communityUuid)
                        ?: return@dbQuery ResultType.Failure(
                            DataError.NotFound
                        )

                val userTypeCode =
                    loadUserTypeCodeInternal(userUuid)
                        ?: return@dbQuery ResultType.Failure(
                            foreignKeyError(
                                operation = operation,
                                message = "Cannot add community member because " +
                                        "user '$userUuid' does not exist."
                            )
                        )

                validateMemberRoleAgainstOwnerInternal(
                    operation = operation,
                    communityId = communityUuid,
                    communityOwnerId = ownerUuid,
                    userId = userUuid,
                    requestedRole = role,
                )?.let { error ->
                    return@dbQuery ResultType.Failure(error)
                }

                if (
                    communityMemberExistsInternal(
                        communityId = communityUuid,
                        userId = userUuid,
                    )
                ) {
                    return@dbQuery ResultType.Failure(
                        duplicateResourceError(
                            operation = operation,
                            message = "Membership already exists. " +
                                    "communityId=$communityUuid, " +
                                    "userId=$userUuid. " +
                                    "Use updateMemberRole to change the role."
                        )
                    )
                }

                val now = Instant.now()

                if (role == CommunityParticipantRole.OWNER) {
                    synchronizeOwnerMembershipInternal(
                        communityId = communityUuid,
                        ownerId = userUuid,
                        now = now,
                    )
                } else {
                    insertMembershipInternal(
                        communityId = communityUuid,
                        userId = userUuid,
                        role = role,
                        userTypeCode = userTypeCode,
                        joinedAt = now,
                    )
                }

                recalculateMemberCountInternal(communityUuid)

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "communityId=$communityUuid, " +
                            "userId=$userUuid, role=$role"
                )
            )
        }
    }

    override suspend fun updateMemberRole(
        communityId: CommunityId,
        userId: UserId,
        role: CommunityParticipantRole,
    ): ResultType<Unit, DataError> {
        val operation = "updateMemberRole"
        val communityUuid = communityId.value
        val userUuid = userId.value

        return try {
            db.dbQuery {
                val ownerUuid =
                    loadCommunityOwnerIdInternal(communityUuid)
                        ?: return@dbQuery ResultType.Failure(
                            DataError.NotFound
                        )

                if (!userExistsInternal(userUuid)) {
                    return@dbQuery ResultType.Failure(
                        foreignKeyError(
                            operation = operation,
                            message = "Cannot update membership because " +
                                    "user '$userUuid' does not exist."
                        )
                    )
                }

                if (
                    !communityMemberExistsInternal(
                        communityId = communityUuid,
                        userId = userUuid,
                    )
                ) {
                    return@dbQuery ResultType.Failure(
                        DataError.NotFound
                    )
                }

                validateMemberRoleAgainstOwnerInternal(
                    operation = operation,
                    communityId = communityUuid,
                    communityOwnerId = ownerUuid,
                    userId = userUuid,
                    requestedRole = role,
                )?.let { error ->
                    return@dbQuery ResultType.Failure(error)
                }

                if (role == CommunityParticipantRole.OWNER) {
                    synchronizeOwnerMembershipInternal(
                        communityId = communityUuid,
                        ownerId = userUuid,
                        now = Instant.now(),
                    )
                } else {
                    CommunityMemberTable.update(
                        where = {
                            (CommunityMemberTable.communityId eq communityUuid) and
                                    (CommunityMemberTable.userId eq userUuid)
                        }
                    ) { row ->
                        row[commParticipantRole] = role.name
                        row[leftAt] = null
                    }
                }

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "communityId=$communityUuid, " +
                            "userId=$userUuid, role=$role"
                )
            )
        }
    }

    override suspend fun addMembers(
        communityId: CommunityId,
        assignments: List<CommunityMemberAssignment>,
    ): ResultType<Unit, DataError> {
        val operation = "addMembers"
        val communityUuid = communityId.value

        if (assignments.isEmpty()) {
            return ResultType.Success(Unit)
        }

        return try {
            db.dbQuery {
                validateBulkAssignmentsShapeInternal(
                    operation = operation,
                    assignments = assignments,
                )?.let { error ->
                    return@dbQuery ResultType.Failure(error)
                }

                val ownerUuid =
                    loadCommunityOwnerIdInternal(communityUuid)
                        ?: return@dbQuery ResultType.Failure(
                            DataError.NotFound
                        )

                val userTypeCodes = loadUserTypeCodesInternal(
                    userIds = assignments
                        .map { assignment ->
                            assignment.userId.value
                        }
                        .toSet(),
                )

                /*
                 * Validate the complete batch before inserting the first row.
                 */
                assignments.forEachIndexed { index, assignment ->
                    val userUuid = assignment.userId.value

                    if (userTypeCodes[userUuid] == null) {
                        return@dbQuery ResultType.Failure(
                            withBulkMemberContext(
                                error = foreignKeyError(
                                    operation = operation,
                                    message = "Cannot add member because user " +
                                            "'$userUuid' does not exist."
                                ),
                                operation = operation,
                                index = index,
                                assignment = assignment,
                                communityId = communityId,
                            )
                        )
                    }

                    validateMemberRoleAgainstOwnerInternal(
                        operation = operation,
                        communityId = communityUuid,
                        communityOwnerId = ownerUuid,
                        userId = userUuid,
                        requestedRole = assignment.role,
                    )?.let { error ->
                        return@dbQuery ResultType.Failure(
                            withBulkMemberContext(
                                error = error,
                                operation = operation,
                                index = index,
                                assignment = assignment,
                                communityId = communityId,
                            )
                        )
                    }

                    if (
                        communityMemberExistsInternal(
                            communityId = communityUuid,
                            userId = userUuid,
                        )
                    ) {
                        return@dbQuery ResultType.Failure(
                            withBulkMemberContext(
                                error = duplicateResourceError(
                                    operation = operation,
                                    message = "Membership already exists for " +
                                            "communityId=$communityUuid and " +
                                            "userId=$userUuid."
                                ),
                                operation = operation,
                                index = index,
                                assignment = assignment,
                                communityId = communityId,
                            )
                        )
                    }
                }

                val now = Instant.now()

                assignments.forEach { assignment ->
                    val userUuid = assignment.userId.value
                    val userTypeCode = requireNotNull(
                        userTypeCodes[userUuid]
                    ) {
                        "Validated community member disappeared before insert. " +
                                "communityId=$communityUuid, userId=$userUuid."
                    }

                    if (
                        assignment.role ==
                        CommunityParticipantRole.OWNER
                    ) {
                        synchronizeOwnerMembershipInternal(
                            communityId = communityUuid,
                            ownerId = userUuid,
                            now = now,
                        )
                    } else {
                        insertMembershipInternal(
                            communityId = communityUuid,
                            userId = userUuid,
                            role = assignment.role,
                            userTypeCode = userTypeCode,
                            joinedAt = now,
                        )
                    }
                }

                recalculateMemberCountInternal(communityUuid)

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "communityId=$communityUuid, " +
                            "assignments.size=${assignments.size}, " +
                            "userIds=${assignments.map { it.userId.value }}"
                )
            )
        }
    }

    override suspend fun removeMember(
        communityId: CommunityId,
        userId: UserId,
    ): ResultType<Unit, DataError> {
        val operation = "removeMember"
        val communityUuid = communityId.value
        val userUuid = userId.value

        return try {
            db.dbQuery {
                val ownerUuid =
                    loadCommunityOwnerIdInternal(communityUuid)
                        ?: return@dbQuery ResultType.Failure(
                            DataError.NotFound
                        )

                if (userUuid == ownerUuid) {
                    return@dbQuery ResultType.Failure(
                        conflictError(
                            operation = operation,
                            message = "Cannot remove user '$userUuid' from " +
                                    "community '$communityUuid' because the " +
                                    "user is the recorded community owner. " +
                                    "Transfer ownership before removing them."
                        )
                    )
                }

                val deletedCount =
                    CommunityMemberTable.deleteWhere {
                        (CommunityMemberTable.communityId eq communityUuid) and
                                (CommunityMemberTable.userId eq userUuid)
                    }

                if (deletedCount == 0) {
                    return@dbQuery ResultType.Failure(
                        DataError.NotFound
                    )
                }

                recalculateMemberCountInternal(communityUuid)

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "communityId=$communityUuid, userId=$userUuid"
                )
            )
        }
    }

    // -----------------------------------------------------------------
    // Read helpers
    // -----------------------------------------------------------------

    private fun loadCommunityByIdInternal(
        communityId: UUID,
    ): Community? {
        val communityRow = CommunityTable
            .selectAll()
            .where {
                CommunityTable.id eq communityId
            }
            .singleOrNull()
            ?: return null

        val socialLinks =
            loadCommunitySocialLinksInternal(
                communityIds = setOf(communityId)
            )[communityId].orEmpty()

        return communityRow
            .toCommunityEntity()
            .toDomain(socialLinks)
    }

    private fun loadCommunitySocialLinksInternal(
        communityIds: Set<UUID>,
    ): Map<UUID, List<SocialLink>> {
        if (communityIds.isEmpty()) {
            return emptyMap()
        }

        return CommunitySocialLinkTable
            .selectAll()
            .where {
                CommunitySocialLinkTable.communityId inList communityIds
            }
            .toList()
            .groupBy { row ->
                row[CommunitySocialLinkTable.communityId]
            }
            .mapValues { (_, rows) ->
                rows.map { row ->
                    row
                        .toCommunitySocialLinkEntity()
                        .toDomain()
                }
            }
    }

    // -----------------------------------------------------------------
    // Write helpers
    // -----------------------------------------------------------------

    private fun insertCommunityInternal(
        community: Community,
        now: Instant,
    ): UUID {
        val entity = community.toEntity(now)

        CommunityTable.insert { row ->
            row[CommunityTable.id] = entity.id
            row[ownerId] = entity.ownerId
            row[name] = entity.name
            row[description] = entity.description
            row[profileUrl] = entity.profileUrl

            /*
             * memberCount is always derived from CommunityMemberTable.
             */
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

        replaceCommunitySocialLinksInternal(
            communityId = entity.id,
            socialLinks = community.socialLinks,
            now = now,
        )

        synchronizeOwnerMembershipInternal(
            communityId = entity.id,
            ownerId = entity.ownerId,
            now = now,
        )

        recalculateMemberCountInternal(entity.id)

        return entity.id
    }

    private fun updateCommunityRowInternal(
        community: Community,
        now: Instant,
    ): Int {
        val entity = community.toEntity(now)

        return CommunityTable.update(
            where = {
                CommunityTable.id eq entity.id
            }
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

            /*
             * createdAt and memberCount are intentionally preserved.
             */
        }
    }

    private fun replaceCommunitySocialLinksInternal(
        communityId: UUID,
        socialLinks: List<SocialLink>,
        now: Instant,
    ) {
        CommunitySocialLinkTable.deleteWhere {
            CommunitySocialLinkTable.communityId eq communityId
        }

        socialLinks.forEach { socialLink ->
            val entity =
                socialLink.toCommunitySocialLinkEntity(
                    communityId = communityId,
                    createdAt = now,
                )

            CommunitySocialLinkTable.insert { row ->
                row[CommunitySocialLinkTable.id] = entity.id
                row[CommunitySocialLinkTable.communityId] =
                    entity.communityId
                row[platformId] = entity.platformId
                row[username] = entity.username
                row[completeUrl] = entity.completeUrl
                row[createdAt] = entity.createdAt
            }
        }
    }

    /**
     * Synchronizes CommunityTable.ownerId with CommunityMemberTable.
     *
     * Any previous OWNER membership is demoted to MEMBER. The target owner
     * is then inserted or updated as OWNER.
     */
    private fun synchronizeOwnerMembershipInternal(
        communityId: UUID,
        ownerId: UUID,
        now: Instant,
    ) {
        CommunityMemberTable.update(
            where = {
                (CommunityMemberTable.communityId eq communityId) and
                        (
                                CommunityMemberTable.commParticipantRole eq
                                        CommunityParticipantRole.OWNER.name
                                )
            }
        ) { row ->
            row[commParticipantRole] =
                CommunityParticipantRole.MEMBER.name
        }

        val existingOwnerMembership =
            CommunityMemberTable
                .selectAll()
                .where {
                    (CommunityMemberTable.communityId eq communityId) and
                            (CommunityMemberTable.userId eq ownerId)
                }
                .singleOrNull()

        if (existingOwnerMembership == null) {
            val userTypeCode =
                requireNotNull(
                    loadUserTypeCodeInternal(ownerId)
                ) {
                    "Cannot synchronize owner membership because the owner " +
                            "user does not exist. " +
                            "communityId=$communityId, ownerId=$ownerId."
                }

            insertMembershipInternal(
                communityId = communityId,
                userId = ownerId,
                role = CommunityParticipantRole.OWNER,
                userTypeCode = userTypeCode,
                joinedAt = now,
            )
        } else {
            CommunityMemberTable.update(
                where = {
                    (CommunityMemberTable.communityId eq communityId) and
                            (CommunityMemberTable.userId eq ownerId)
                }
            ) { row ->
                row[commParticipantRole] =
                    CommunityParticipantRole.OWNER.name
                row[leftAt] = null
            }
        }
    }

    private fun insertMembershipInternal(
        communityId: UUID,
        userId: UUID,
        role: CommunityParticipantRole,
        userTypeCode: Int,
        joinedAt: Instant,
    ) {
        CommunityMemberTable.insert { row ->
            row[CommunityMemberTable.communityId] = communityId
            row[CommunityMemberTable.userId] = userId
            row[CommunityMemberTable.joinedAt] = joinedAt
            row[CommunityMemberTable.leftAt] = null
            row[CommunityMemberTable.userTypeAtJoin] = userTypeCode
            row[CommunityMemberTable.commParticipantRole] = role.name
        }
    }

    private fun recalculateMemberCountInternal(
        communityId: UUID,
    ) {
        val memberCount = CommunityMemberTable
            .selectAll()
            .where {
                CommunityMemberTable.communityId eq communityId
            }
            .count()
            .toInt()

        val updatedCount = CommunityTable.update(
            where = {
                CommunityTable.id eq communityId
            }
        ) { row ->
            row[CommunityTable.memberCount] = memberCount
        }

        check(updatedCount == 1) {
            "Failed to recalculate community member count because the " +
                    "community row was not updated. " +
                    "communityId=$communityId, calculatedMemberCount=$memberCount, " +
                    "updatedRows=$updatedCount."
        }
    }

    private fun communityWriteDetails(
        community: Community,
    ): String =
        "community.id=${community.id}, " +
                "community.name=${community.name}, " +
                "ownerId=${community.ownerId.value}, " +
                "memberCount=${community.memberCount}, " +
                "socialLinks.size=${community.socialLinks.size}"

    // -----------------------------------------------------------------
    // Legacy adapters
    //
    // -----------------------------------------------------------------

    @Suppress("DEPRECATION")
    override suspend fun getAllCommunities(
        approved: Boolean?,
        ownerId: String?,
    ): ResultType<List<Community>, DataError> {
        val parsedOwnerId = if (ownerId.isNullOrBlank()) {
            null
        } else {
            when (
                val parsed = parseUuidOrFailure(
                    operation = "getAllCommunities",
                    field = "ownerId",
                    value = ownerId,
                )
            ) {
                is ResultType.Success ->
                    UserId(parsed.data)

                is ResultType.Failure ->
                    return parsed
            }
        }

        return getCommunities(
            approved = approved,
            ownerId = parsedOwnerId,
        )
    }

    @Suppress("DEPRECATION")
    override suspend fun getCommunityById(
        communityId: String?,
    ): ResultType<Community, DataError> {
        val typedCommunityId = when (
            val parsed = parseUuidOrFailure(
                operation = "getCommunityById",
                field = "communityId",
                value = communityId,
            )
        ) {
            is ResultType.Success ->
                CommunityId(parsed.data)

            is ResultType.Failure ->
                return parsed
        }

        return getCommunityById(typedCommunityId)
    }

    @Suppress("DEPRECATION")
    override suspend fun upsertCommunity(
        community: Community,
    ): ResultType<Unit, DataError> =
        if (community.id == null) {
            createCommunity(community)
        } else {
            updateCommunity(community)
        }

    @Suppress("DEPRECATION")
    override suspend fun deleteCommunityById(
        communityId: String?,
    ): ResultType<Unit, DataError> {
        val typedCommunityId = when (
            val parsed = parseUuidOrFailure(
                operation = "deleteCommunityById",
                field = "communityId",
                value = communityId,
            )
        ) {
            is ResultType.Success ->
                CommunityId(parsed.data)

            is ResultType.Failure ->
                return parsed
        }

        return deleteCommunityById(typedCommunityId)
    }

    @Suppress("DEPRECATION")
    override suspend fun insertCommunitiesInBulk(
        communities: List<Community>,
    ): ResultType<Unit, DataError> =
        createCommunities(communities)

    @Suppress("DEPRECATION")
    override suspend fun listMembers(
        communityId: String?,
    ): ResultType<List<CommunityMember>, DataError> {
        val typedCommunityId = when (
            val parsed = parseUuidOrFailure(
                operation = "listMembers",
                field = "communityId",
                value = communityId,
            )
        ) {
            is ResultType.Success ->
                CommunityId(parsed.data)

            is ResultType.Failure ->
                return parsed
        }

        return getMembers(typedCommunityId)
    }

    @Suppress("DEPRECATION")
    override suspend fun upsertMember(
        communityId: String?,
        userId: String?,
        role: CommunityParticipantRole,
    ): ResultType<Unit, DataError> {
        val typedCommunityId = when (
            val parsed = parseUuidOrFailure(
                operation = "upsertMember",
                field = "communityId",
                value = communityId,
            )
        ) {
            is ResultType.Success ->
                CommunityId(parsed.data)

            is ResultType.Failure ->
                return parsed
        }

        val typedUserId = when (
            val parsed = parseUuidOrFailure(
                operation = "upsertMember",
                field = "userId",
                value = userId,
            )
        ) {
            is ResultType.Success ->
                UserId(parsed.data)

            is ResultType.Failure ->
                return parsed
        }

        val membershipExists = try {
            db.dbQuery {
                communityMemberExistsInternal(
                    communityId = typedCommunityId.value,
                    userId = typedUserId.value,
                )
            }
        } catch (e: Exception) {
            return ResultType.Failure(
                databaseError(
                    operation = "upsertMember",
                    e = e,
                    details = "communityId=${typedCommunityId.value}, " +
                            "userId=${typedUserId.value}, role=$role"
                )
            )
        }

        return if (membershipExists) {
            updateMemberRole(
                communityId = typedCommunityId,
                userId = typedUserId,
                role = role,
            )
        } else {
            addMember(
                communityId = typedCommunityId,
                userId = typedUserId,
                role = role,
            )
        }
    }

    @Suppress("DEPRECATION")
    override suspend fun upsertMembersInBulk(
        communityId: String?,
        members: List<CommunityMember>,
    ): ResultType<Unit, DataError> {
        val typedCommunityId = when (
            val parsed = parseUuidOrFailure(
                operation = "upsertMembersInBulk",
                field = "communityId",
                value = communityId,
            )
        ) {
            is ResultType.Success ->
                CommunityId(parsed.data)

            is ResultType.Failure ->
                return parsed
        }

        val mismatchedCommunityIndex =
            members.indexOfFirst { member ->
                member.communityId != typedCommunityId
            }

        if (mismatchedCommunityIndex >= 0) {
            val member = members[mismatchedCommunityIndex]

            return ResultType.Failure(
                validationError(
                    operation = "upsertMembersInBulk",
                    field = "members[$mismatchedCommunityIndex].communityId",
                    value = member.communityId.value.toString(),
                    reason = "Member community ID does not match the " +
                            "community ID supplied by the route. " +
                            "expectedCommunityId=${typedCommunityId.value}."
                )
            )
        }

        val assignments = members.map { member ->
            CommunityMemberAssignment(
                userId = member.userId,
                role = member.commParticipantRole,
            )
        }

        /*
         * Temporary compatibility behavior:
         *
         * The old endpoint was an upsert operation. Existing memberships are
         * updated, while missing memberships are added. Phase 3 will replace
         * this endpoint with explicit application operations.
         */
        assignments.forEach { assignment ->
            when (
                val result = upsertMember(
                    communityId = typedCommunityId.value.toString(),
                    userId = assignment.userId.value.toString(),
                    role = assignment.role,
                )
            ) {
                is ResultType.Success ->
                    Unit

                is ResultType.Failure ->
                    return result
            }
        }

        return ResultType.Success(Unit)
    }

    @Suppress("DEPRECATION")
    override suspend fun removeMember(
        communityId: String?,
        userId: String?,
    ): ResultType<Unit, DataError> {
        val typedCommunityId = when (
            val parsed = parseUuidOrFailure(
                operation = "removeMember",
                field = "communityId",
                value = communityId,
            )
        ) {
            is ResultType.Success ->
                CommunityId(parsed.data)

            is ResultType.Failure ->
                return parsed
        }

        val typedUserId = when (
            val parsed = parseUuidOrFailure(
                operation = "removeMember",
                field = "userId",
                value = userId,
            )
        ) {
            is ResultType.Success ->
                UserId(parsed.data)

            is ResultType.Failure ->
                return parsed
        }

        return removeMember(
            communityId = typedCommunityId,
            userId = typedUserId,
        )
    }
}