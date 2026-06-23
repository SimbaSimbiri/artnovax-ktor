package com.simbiri.data.repository

import com.simbiri.data.database.entity.social.SocialLinkTable
import com.simbiri.data.database.entity.social.SocialPlatformTable
import com.simbiri.data.database.entity.user.UserTable
import com.simbiri.data.database.utils.dbQuery
import com.simbiri.data.mapper.social.toDomain
import com.simbiri.data.mapper.social.toEntity
import com.simbiri.data.mapper.social.toSocialLinkEntity
import com.simbiri.data.mapper.social.toSocialPlatformEntity
import com.simbiri.data.mapper.user.toDomain
import com.simbiri.data.mapper.user.toEntity
import com.simbiri.data.mapper.user.toUserEntity
import com.simbiri.data.repository.util.*
import com.simbiri.domain.model.social.SocialLink
import com.simbiri.domain.model.user.User
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import java.util.UUID

class UserRepoImpl(
    private val db: Database,
) : UserRepository {

    // -------- Error helpers --------

    private fun withBulkContext(
        error: DataError,
        operation: String,
        index: Int,
        user: User,
    ): DataError {
        val context = "Bulk user upsert failed in $operation at index=$index. " +
                "user.id=${user.id}, accountName=${user.accountName}, emailAddress=${user.emailAddress}."

        return when (error) {
            DataError.NotFound -> DataError.NotFound

            is DataError.ValidationError ->
                DataError.ValidationError("$context \nNested validation error: ${error.message}")

            is DataError.DatabaseError ->
                DataError.DatabaseError(
                    operation = operation,
                    cause = "$context \nNested database error from ${error.operation}: ${error.cause}"
                )

            is DataError.ForeignKeyViolation ->
                DataError.ForeignKeyViolation("$context \nNested foreign key error: ${error.message}")

            is DataError.Conflict ->
                DataError.Conflict("$context \nNested conflict error: ${error.message}")

            is DataError.DuplicateResource ->
                DataError.DuplicateResource("$context \nNested duplicate resource error: ${error.message}")

            is DataError.UnknownError ->
                DataError.UnknownError("$context \nNested unknown error: ${error.cause}")
        }
    }

    // -------- Internal existence helpers --------

    private fun socialPlatformExistsInternal(platformId: Int): Boolean =
        SocialPlatformTable
            .selectAll()
            .where { SocialPlatformTable.id eq EntityID(table= SocialPlatformTable, id=platformId )}
            .limit(1)
            .any()

    // -------- Validation helpers --------

    /**
     * Validates user state before insert/update.
     *
     * Important:
     * - if canExposeSocialLinks=false, the user should not submit social links.
     * - Bulk insert also calls this method, so bulk uploads cannot bypass this rule.
     * - Social platform IDs are checked before writing, so FK failures become readable errors.
     *
     * Must be called inside dbQuery / transaction because it can query SocialPlatformTable.
     */
    private fun validateUserForUpsertInternal(
        operation: String,
        user: User,
    ): DataError? {
        if (!user.canExposeSocialLinks && user.socialLinks.isNotEmpty()) {
            return validationError(
                operation = operation,
                field = "socialLinks",
                value = "socialLinks.size=${user.socialLinks.size}, canExposeSocialLinks=${user.canExposeSocialLinks}",
                reason = "User cannot have social links when canExposeSocialLinks is false."
            )
        }

        if (user.canExposeSocialLinks) {
            user.socialLinks.forEachIndexed { index, socialLink ->
                val platformId = socialLink.platform.id

                if (!socialPlatformExistsInternal(platformId)) {
                    return foreignKeyError(
                        operation = operation,
                        message = "Social link at index=$index references platform '$platformId', " +
                                "but no row exists in social platform table for that platform id."
                    )
                }
            }
        }

        return null
    }

    // -------- Users CRUD --------

    override suspend fun getAllUsers(
        userType: Int?,
    ): ResultType<List<User>, DataError> {
        val operation = "getAllUsers"

        return try {
            val users = db.dbQuery {
                val baseQuery = if (userType == null) {
                    UserTable.selectAll()
                } else {
                    UserTable.selectAll().where { UserTable.userType eq userType }
                }

                val userRows = baseQuery.toList()
                if (userRows.isEmpty()) {
                    return@dbQuery emptyList<User>()
                }

                val userEntities = userRows.map { it.toUserEntity() }
                val userIds = userEntities.map { it.id }.toSet()

                val userIdEntityIds = userIds.map { EntityID(it, UserTable) }

                val socialRows = SocialLinkTable
                    .join(
                        SocialPlatformTable,
                        JoinType.INNER,
                        onColumn = SocialLinkTable.platformId,
                        otherColumn = SocialPlatformTable.id
                    )
                    .selectAll()
                    .where { SocialLinkTable.userId inList userIdEntityIds }
                    .toList()

                val socialsByUserId: Map<UUID, List<SocialLink>> =
                    socialRows
                        .groupBy { row -> row[SocialLinkTable.userId].value }
                        .mapValues { (_, rows) ->
                            rows.map { row ->
                                val platform = row.toSocialPlatformEntity().toDomain()
                                row.toSocialLinkEntity().toDomain(platform)
                            }
                        }

                userEntities.map { entity ->
                    val socials = socialsByUserId[entity.id].orEmpty()
                    entity.toDomain(socialLinks = socials)
                }
            }

            if (users.isEmpty()) {
                ResultType.Failure(DataError.NotFound)
            } else {
                ResultType.Success(users)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "userType=$userType"
                )
            )
        }
    }

    override suspend fun getUserById(
        userId: String?,
    ): ResultType<User, DataError> {
        val operation = "getUserById"

        val uuid = when (
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
            val user = db.dbQuery {
                val userRow = UserTable
                    .selectAll()
                    .where { UserTable.id eq uuid }
                    .singleOrNull()
                    ?: return@dbQuery null

                val userEntity = userRow.toUserEntity()

                val socialRows = SocialLinkTable
                    .join(
                        SocialPlatformTable,
                        JoinType.INNER,
                        onColumn = SocialLinkTable.platformId,
                        otherColumn = SocialPlatformTable.id
                    )
                    .selectAll()
                    .where { SocialLinkTable.userId eq EntityID(uuid, UserTable) }
                    .toList()

                val socialLinks = socialRows.map { row ->
                    val platform = row.toSocialPlatformEntity().toDomain()
                    row.toSocialLinkEntity().toDomain(platform)
                }

                userEntity.toDomain(socialLinks = socialLinks)
            }

            if (user == null) {
                ResultType.Failure(DataError.NotFound)
            } else {
                ResultType.Success(user)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "userId=$userId, parsedUserUuid=$uuid"
                )
            )
        }
    }

    override suspend fun upsertUser(
        userRec: User,
    ): ResultType<Unit, DataError> {
        val operation = "upsertUser"

        return try {
            db.dbQuery {
                upsertUserInternal(userRec)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "user.id=${userRec.id}, accountName=${userRec.accountName}, " +
                            "emailAddress=${userRec.emailAddress}, socialLinks.size=${userRec.socialLinks.size}, " +
                            "canExposeSocialLinks=${userRec.canExposeSocialLinks}"
                )
            )
        }
    }

    private fun upsertUserInternal(
        userRec: User,
    ): ResultType<Unit, DataError> {
        val operation = "upsertUserInternal"
        val now = Instant.now()
        val userEntity = userRec.toEntity(now)

        validateUserForUpsertInternal(
            operation = operation,
            user = userRec,
        )?.let { error ->
            return ResultType.Failure(error)
        }

        if (userRec.id == null) {
            UserTable.insert { row ->
                row[UserTable.id] = userEntity.id
                row[accountName] = userEntity.accountName
                row[emailAddress] = userEntity.emailAddress
                row[firstName] = userEntity.firstName
                row[lastName] = userEntity.lastName
                row[birthDate] = userEntity.birthDate
                row[about] = userEntity.about
                row[tagline] = userEntity.tagline
                row[profileUrl] = userEntity.profileUrl
                row[backgroundUrl] = userEntity.backgroundUrl
                row[userType] = userEntity.userTypeCode
                row[emailOptIn] = userEntity.emailOptIn
                row[isPrivate] = userEntity.isPrivate
                row[isAnonymous] = userEntity.isAnonymous
                row[isActive] = userEntity.isActive
                row[createdAt] = userEntity.createdAt
                row[updatedAt] = userEntity.updatedAt
            }

            upsertSocialLinksForUserInternal(
                userId = userEntity.id,
                user = userRec,
            )
        } else {
            val updatedCount = UserTable.update(
                where = { UserTable.id eq userEntity.id }
            ) { row ->
                row[accountName] = userEntity.accountName
                row[emailAddress] = userEntity.emailAddress
                row[firstName] = userEntity.firstName
                row[lastName] = userEntity.lastName
                row[birthDate] = userEntity.birthDate
                row[about] = userEntity.about
                row[tagline] = userEntity.tagline
                row[profileUrl] = userEntity.profileUrl
                row[backgroundUrl] = userEntity.backgroundUrl
                row[userType] = userEntity.userTypeCode
                row[emailOptIn] = userEntity.emailOptIn
                row[isPrivate] = userEntity.isPrivate
                row[isAnonymous] = userEntity.isAnonymous
                row[isActive] = userEntity.isActive
                row[updatedAt] = userEntity.updatedAt
            }

            if (updatedCount == 0) {
                return ResultType.Failure(DataError.NotFound)
            }

            upsertSocialLinksForUserInternal(
                userId = userEntity.id,
                user = userRec,
            )
        }

        return ResultType.Success(Unit)
    }

    override suspend fun insertUsersInBulk(
        users: List<User>,
    ): ResultType<Unit, DataError> {
        val operation = "insertUsersInBulk"

        if (users.isEmpty()) {
            return ResultType.Success(Unit)
        }

        return try {
            db.dbQuery {
                users.forEachIndexed { index, user ->
                    when (val res = upsertUserInternal(user)) {
                        is ResultType.Failure -> {
                            return@dbQuery ResultType.Failure(
                                withBulkContext(
                                    error = res.error,
                                    operation = operation,
                                    index = index,
                                    user = user,
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
                    details = "users.size=${users.size}"
                )
            )
        }
    }

    override suspend fun deleteUserById(
        userId: String?,
    ): ResultType<Unit, DataError> {
        val operation = "deleteUserById"

        val uuid = when (
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
                SocialLinkTable.deleteWhere {
                    SocialLinkTable.userId eq EntityID(uuid, UserTable)
                }

                val deletedCount = UserTable.deleteWhere {
                    UserTable.id eq uuid
                }

                if (deletedCount > 0) {
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
                    details = "userId=$userId, parsedUserUuid=$uuid"
                )
            )
        }
    }

    // -------- Social link helpers --------

    /**
     * Replaces the user's social links.
     *
     * Must be called inside dbQuery / transaction.
     * Validation is expected to happen before this function via validateUserForUpsertInternal.
     */
    private fun upsertSocialLinksForUserInternal(
        userId: UUID,
        user: User,
    ) {
        if (!user.canExposeSocialLinks) {
            SocialLinkTable.deleteWhere {
                SocialLinkTable.userId eq EntityID(userId, UserTable)
            }
            return
        }

        SocialLinkTable.deleteWhere {
            SocialLinkTable.userId eq EntityID(userId, UserTable)
        }

        user.socialLinks.forEach { socialLink ->
            val socialLinkEntity = socialLink.toEntity(
                userId = userId,
                platformId = socialLink.platform.id,
            )

            SocialLinkTable.insert { row ->
                row[SocialLinkTable.id] = socialLinkEntity.id
                row[SocialLinkTable.userId] = EntityID(socialLinkEntity.userId, UserTable)
                row[platformId] = EntityID(socialLinkEntity.platformId, SocialPlatformTable)
                row[username] = socialLinkEntity.username
                row[completeUrl] = socialLinkEntity.completeUrl
                row[createdAt] = socialLinkEntity.createdAt
            }
        }
    }
}