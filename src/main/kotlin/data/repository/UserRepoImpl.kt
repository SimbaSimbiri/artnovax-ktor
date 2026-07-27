package com.simbiri.data.repository

import com.simbiri.data.database.entity.social.SocialLinkTable
import com.simbiri.data.database.entity.social.SocialPlatformTable
import com.simbiri.data.database.entity.user.UserEntity
import com.simbiri.data.database.entity.user.UserTable
import com.simbiri.data.database.utils.dbQuery
import com.simbiri.data.mapper.social.toDomain
import com.simbiri.data.mapper.social.toEntity
import com.simbiri.data.mapper.social.toSocialLinkEntity
import com.simbiri.data.mapper.social.toSocialPlatformEntity
import com.simbiri.data.mapper.user.toDomain
import com.simbiri.data.mapper.user.toEntity
import com.simbiri.data.mapper.user.toUserEntity
import com.simbiri.data.repository.util.databaseError
import com.simbiri.data.repository.util.foreignKeyError
import com.simbiri.data.repository.util.validationError
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.social.SocialLink
import com.simbiri.domain.model.user.User
import com.simbiri.domain.model.user.UserType
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import java.util.*

class UserRepoImpl(
    private val db: Database,
) : UserRepository {

    // -----------------------------------------------------------------
    // Error context
    // -----------------------------------------------------------------

    private fun withBulkContext(
        error: DataError,
        operation: String,
        index: Int,
        user: User,
    ): DataError {
        val context =
            "Bulk user creation failed in $operation at index=$index. " +
                    "user.id=${user.id}, " +
                    "accountName=${user.accountName}, " +
                    "emailAddress=${user.emailAddress}."

        return when (error) {
            DataError.NotFound -> DataError.NotFound

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

            is DataError.Forbidden ->
                DataError.Forbidden(
                    "$context Nested authorization error: ${error.message}"
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
    }

    // -----------------------------------------------------------------
    // Persistence validation
    // -----------------------------------------------------------------

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


    /**
     * Validates database-dependent constraints before a user write.
     *
     * Domain rules are enforced by UserPolicy in the application layer.
     * This repository validates only constraints that require database access.
     *
     * This function must run inside dbQuery because it reads
     * SocialPlatformTable.
     */
    private fun validateUserForPersistenceInternal(
        operation: String,
        user: User,
    ): DataError? {
        user.socialLinks.forEachIndexed { index, socialLink ->
            val platformId = socialLink.platform.id

            if (!socialPlatformExistsInternal(platformId)) {
                return foreignKeyError(
                    operation = operation,
                    message = "socialLinks[$index] references " +
                            "socialPlatformId=$platformId, but no matching " +
                            "row exists in SocialPlatformTable."
                )
            }
        }

        return null
    }

    // -----------------------------------------------------------------
    // Typed repository API
    // -----------------------------------------------------------------

    override suspend fun getUsers(
        userType: UserType?,
    ): ResultType<List<User>, DataError> {
        val operation = "getUsers"

        return try {
            val users = db.dbQuery {
                val query = if (userType == null) {
                    UserTable.selectAll()
                } else {
                    UserTable
                        .selectAll()
                        .where {
                            UserTable.userType eq userType.code
                        }
                }

                val userEntities = query
                    .toList()
                    .map(ResultRow::toUserEntity)

                if (userEntities.isEmpty()) {
                    return@dbQuery emptyList<User>()
                }

                val socialsByUserId =
                    loadSocialLinksByUserIdsInternal(
                        userIds = userEntities
                            .map { entity -> entity.id }
                            .toSet(),
                    )

                userEntities.map { entity ->
                    entity.toDomain(
                        socialLinks = socialsByUserId[entity.id].orEmpty()
                    )
                }
            }

            if (users.isEmpty()) {
                ResultType.Failure(DataError.NotFound)
            } else {
                ResultType.Success(users)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "userType=$userType, " +
                            "userTypeCode=${userType?.code}"
                )
            )
        }
    }

    override suspend fun getUserById(
        userId: UserId,
    ): ResultType<User, DataError> {
        val operation = "getUserById"

        return try {
            val user = db.dbQuery {
                loadUserByIdInternal(userId.value)
            }

            if (user == null) {
                ResultType.Failure(DataError.NotFound)
            } else {
                ResultType.Success(user)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "userId=${userId.value}"
                )
            )
        }
    }

    override suspend fun createUser(
        user: User,
    ): ResultType<Unit, DataError> {
        val operation = "createUser"

        if (user.id != null) {
            return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "user.id",
                    value = user.id.value.toString(),
                    reason = "A new user must not already have an ID."
                )
            )
        }

        return try {
            db.dbQuery {
                validateUserForPersistenceInternal(
                    operation = operation,
                    user = user,
                )?.let { error ->
                    return@dbQuery ResultType.Failure(error)
                }

                insertUserInternal(
                    user = user,
                    now = Instant.now(),
                )

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = userWriteDetails(user)
                )
            )
        }
    }

    override suspend fun updateUser(
        user: User,
    ): ResultType<Unit, DataError> {
        val operation = "updateUser"

        val userId = user.id?.value
            ?: return ResultType.Failure(
                validationError(
                    operation = operation,
                    field = "user.id",
                    value = null,
                    reason = "An existing user ID is required for update."
                )
            )

        return try {
            db.dbQuery {
                validateUserForPersistenceInternal(
                    operation = operation,
                    user = user,
                )?.let { error ->
                    return@dbQuery ResultType.Failure(error)
                }

                val updatedCount = updateUserInternal(
                    user = user,
                    now = Instant.now(),
                )

                if (updatedCount == 0) {
                    return@dbQuery ResultType.Failure(
                        DataError.NotFound
                    )
                }

                replaceSocialLinksForUserInternal(
                    userId = userId,
                    user = user,
                )

                ResultType.Success(Unit)
            }
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = userWriteDetails(user)
                )
            )
        }
    }

    override suspend fun createUsers(
        users: List<User>,
    ): ResultType<Unit, DataError> {
        val operation = "createUsers"

        if (users.isEmpty()) {
            return ResultType.Success(Unit)
        }

        return try {
            db.dbQuery {
                /*
                 * Validate the complete batch before the first insert.
                 *
                 * Returning Failure after some inserts have occurred may
                 * allow a transaction to commit those earlier writes.
                 */
                users.forEachIndexed { index, user ->
                    if (user.id != null) {
                        return@dbQuery ResultType.Failure(
                            withBulkContext(
                                error = validationError(
                                    operation = operation,
                                    field = "users[$index].id",
                                    value = user.id.value.toString(),
                                    reason = "Bulk creation only accepts " +
                                            "users without existing IDs."
                                ),
                                operation = operation,
                                index = index,
                                user = user,
                            )
                        )
                    }

                    validateUserForPersistenceInternal(
                        operation = operation,
                        user = user,
                    )?.let { error ->
                        return@dbQuery ResultType.Failure(
                            withBulkContext(
                                error = error,
                                operation = operation,
                                index = index,
                                user = user,
                            )
                        )
                    }
                }

                val now = Instant.now()

                users.forEach { user ->
                    insertUserInternal(
                        user = user,
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
                    details = "users.size=${users.size}, " +
                            "accountNames=${users.map { it.accountName }}"
                )
            )
        }
    }

    override suspend fun deleteUserById(
        userId: UserId,
    ): ResultType<Unit, DataError> {
        val operation = "deleteUserById"
        val uuid = userId.value

        return try {
            db.dbQuery {
                /*
                 * Keep dependent deletion and user deletion in the same
                 * transaction.
                 */
                SocialLinkTable.deleteWhere {
                    SocialLinkTable.userId eq EntityID(
                        id = uuid,
                        table = UserTable,
                    )
                }

                val deletedCount = UserTable.deleteWhere {
                    UserTable.id eq uuid
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
                    details = "userId=$uuid"
                )
            )
        }
    }

    // -----------------------------------------------------------------
    // Read helpers
    // -----------------------------------------------------------------

    private fun loadUserByIdInternal(
        userId: UUID,
    ): User? {
        val userRow = UserTable
            .selectAll()
            .where {
                UserTable.id eq userId
            }
            .singleOrNull()
            ?: return null

        val socialLinks = loadSocialLinksByUserIdsInternal(
            userIds = setOf(userId)
        )[userId].orEmpty()

        return userRow
            .toUserEntity()
            .toDomain(socialLinks)
    }

    private fun loadSocialLinksByUserIdsInternal(
        userIds: Set<UUID>,
    ): Map<UUID, List<SocialLink>> {
        if (userIds.isEmpty()) {
            return emptyMap()
        }

        val userEntityIds = userIds.map { userId ->
            EntityID(
                id = userId,
                table = UserTable,
            )
        }

        return SocialLinkTable
            .join(
                otherTable = SocialPlatformTable,
                joinType = JoinType.INNER,
                onColumn = SocialLinkTable.platformId,
                otherColumn = SocialPlatformTable.id,
            )
            .selectAll()
            .where {
                SocialLinkTable.userId inList userEntityIds
            }
            .toList()
            .groupBy { row ->
                row[SocialLinkTable.userId].value
            }
            .mapValues { (_, rows) ->
                rows.map { row ->
                    val platform = row
                        .toSocialPlatformEntity()
                        .toDomain()

                    row
                        .toSocialLinkEntity()
                        .toDomain(platform)
                }
            }
    }

    // -----------------------------------------------------------------
    // Write helpers
    // -----------------------------------------------------------------

    private fun insertUserInternal(
        user: User,
        now: Instant,
    ): UUID {
        val userEntity = user.toEntity(now)

        insertUserRowInternal(userEntity)

        replaceSocialLinksForUserInternal(
            userId = userEntity.id,
            user = user,
        )

        return userEntity.id
    }

    private fun insertUserRowInternal(
        userEntity: UserEntity,
    ) {
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
    }

    private fun updateUserInternal(
        user: User,
        now: Instant,
    ): Int {
        val userEntity = user.toEntity(now)

        return UserTable.update(
            where = {
                UserTable.id eq userEntity.id
            }
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
    }

    /**
     * Replaces all social links belonging to one user.
     *
     * This must run inside the surrounding transaction.
     */
    private fun replaceSocialLinksForUserInternal(
        userId: UUID,
        user: User,
    ) {
        SocialLinkTable.deleteWhere {
            SocialLinkTable.userId eq EntityID(
                id = userId,
                table = UserTable,
            )
        }

        if (!user.canExposeSocialLinks) {
            return
        }

        user.socialLinks.forEach { socialLink ->
            val socialLinkEntity = socialLink.toEntity(
                userId = userId,
                platformId = socialLink.platform.id,
            )

            SocialLinkTable.insert { row ->
                row[SocialLinkTable.id] = socialLinkEntity.id
                row[SocialLinkTable.userId] = EntityID(
                    id = socialLinkEntity.userId,
                    table = UserTable,
                )
                row[platformId] = EntityID(
                    id = socialLinkEntity.platformId,
                    table = SocialPlatformTable,
                )
                row[username] = socialLinkEntity.username
                row[completeUrl] = socialLinkEntity.completeUrl
                row[createdAt] = socialLinkEntity.createdAt
            }
        }
    }

    private fun userWriteDetails(
        user: User,
    ): String =
        "user.id=${user.id}, " +
                "accountName=${user.accountName}, " +
                "emailAddress=${user.emailAddress}, " +
                "userType=${user.type}, " +
                "socialLinks.size=${user.socialLinks.size}"
}