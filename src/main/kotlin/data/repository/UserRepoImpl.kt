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
import com.simbiri.domain.model.social.SocialLink
import com.simbiri.domain.model.user.User
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import java.util.*


class UserRepoImpl (private val db: Database) : UserRepository {

    override suspend fun getAllUsers(userType: Int?): ResultType<List<User>, DataError> {
        return try {
            val users = db.dbQuery {
                // 1) Fetch all users (optionally filtered by type)
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

                // 2) Fetch all social links for those users in one go
                val userIdEntityIds = userIds.map { EntityID(it, UserTable) }

                val socialRows = SocialLinkTable
                    .join(
                        SocialPlatformTable,
                        JoinType.INNER,
                        onColumn = SocialLinkTable.platformId,
                        otherColumn = SocialPlatformTable.id
                    )
                    .selectAll().where { SocialLinkTable.userId inList userIdEntityIds }
                    .toList()

                // 3) Group socials by userId
                val socialsByUserId: Map<UUID, List<SocialLink>> =
                    socialRows
                        .groupBy { row -> row[SocialLinkTable.userId].value }
                        .mapValues { (_, rows) ->
                            rows.map { row ->
                                val platform = row.toSocialPlatformEntity().toDomain()
                                row.toSocialLinkEntity().toDomain(platform)
                            }
                        }

                // 4) Build domain Users with their social links
                userEntities.map { entity ->
                    val socials = socialsByUserId[entity.id].orEmpty()
                    entity.toDomain(socialLinks = socials)
                }
            }

            if (users.isEmpty()) ResultType.Failure(DataError.NotFound)
            else ResultType.Success(users)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(DataError.DatabaseError)
        }
    }

    override suspend fun getUserById(userId: String?): ResultType<User, DataError> {
        if (userId.isNullOrBlank()) {
            return ResultType.Failure(DataError.ValidationError)
        }

        val uuid = try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            return ResultType.Failure(DataError.ValidationError)
        }

        return try {
            val user = db.dbQuery {
                val userRow = UserTable
                    .selectAll().where { UserTable.id eq uuid }
                    .singleOrNull()
                    ?: return@dbQuery null

                val userEntity = userRow.toUserEntity()

                // Load socials for this user
                val socialRows = SocialLinkTable
                    .join(
                        SocialPlatformTable,
                        JoinType.INNER,
                        onColumn = SocialLinkTable.platformId,
                        otherColumn = SocialPlatformTable.id
                    )
                    .selectAll().where { SocialLinkTable.userId eq EntityID(uuid, UserTable) }
                    .toList()

                val socialLinks = socialRows.map { row ->
                    val platform = row.toSocialPlatformEntity().toDomain()
                    row.toSocialLinkEntity().toDomain(platform)
                }

                userEntity.toDomain(socialLinks = socialLinks)
            }

            if (user == null) ResultType.Failure(DataError.NotFound)
            else ResultType.Success(user)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(DataError.DatabaseError)
        }
    }

    override suspend fun upsertUser(userRec: User): ResultType<Unit, DataError> {
        if (!userRec.canExposeSocialLinks && userRec.socialLinks.isNotEmpty()) {
            return ResultType.Failure(DataError.ValidationError)
        }

        return try {
            db.dbQuery {
                upsertUserInternal(userRec)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultType.Failure(DataError.DatabaseError)
        }
    }

    private fun upsertUserInternal(userRec: User): ResultType<Unit, DataError> {
        val now = Instant.now()
        val entity = userRec.toEntity(now)   // <- use the mapper here

        if (userRec.id == null) {
            // INSERT path
            UserTable.insert { row ->
                row[UserTable.id] = entity.id
                row[accountName] = entity.accountName
                row[emailAddress] = entity.emailAddress
                row[firstName] = entity.firstName
                row[lastName] = entity.lastName
                row[birthDate] = entity.birthDate
                row[about] = entity.about
                row[tagline] = entity.tagline
                row[profileUrl] = entity.profileUrl
                row[backgroundUrl] = entity.backgroundUrl
                row[userType] = entity.userTypeCode
                row[emailOptIn] = entity.emailOptIn
                row[isPrivate] = entity.isPrivate
                row[isAnonymous] = entity.isAnonymous
                row[isActive] = entity.isActive
                row[createdAt] = entity.createdAt
                row[updatedAt] = entity.updatedAt
            }

            upsertSocialLinksForUserInternal(
                userId = entity.id,
                user = userRec,
                now = now
            )
        } else {
            // UPDATE path
            val updatedCount = UserTable.update(
                where = { UserTable.id eq entity.id }
            ) { row ->
                row[accountName] = entity.accountName
                row[emailAddress] = entity.emailAddress
                row[firstName] = entity.firstName
                row[lastName] = entity.lastName
                row[birthDate] = entity.birthDate
                row[about] = entity.about
                row[tagline] = entity.tagline
                row[profileUrl] = entity.profileUrl
                row[backgroundUrl] = entity.backgroundUrl
                row[userType] = entity.userTypeCode
                row[emailOptIn] = entity.emailOptIn
                row[isPrivate] = entity.isPrivate
                row[isAnonymous] = entity.isAnonymous
                row[isActive] = entity.isActive
                row[updatedAt] = entity.updatedAt
            }

            if (updatedCount == 0) {
                return ResultType.Failure<DataError>(DataError.NotFound)
            }

            upsertSocialLinksForUserInternal(
                userId = entity.id,
                user = userRec,
                now = now
            )
        }

        return ResultType.Success(Unit)
    }

    override suspend fun insertUsersInBulk(users: List<User>): ResultType<Unit, DataError> {
        return try {
            db.dbQuery {
                users.forEach { user ->
                    when (val res = upsertUserInternal(user)) {
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
    }

    override suspend fun deleteUserById(userId: String?): ResultType<Unit, DataError> {
        if (userId.isNullOrBlank()) {
            return ResultType.Failure(DataError.ValidationError)
        }

        val uuid = try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            return ResultType.Failure(DataError.ValidationError)
        }

        return try {
            db.dbQuery {
                // Delete socials first (due to FK)
                SocialLinkTable.deleteWhere {
                    SocialLinkTable.userId eq EntityID(uuid, UserTable)
                }

                val deletedCount = UserTable.deleteWhere { UserTable.id eq uuid }

                if (deletedCount > 0) {
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

    // Our Internal helpers
    private fun upsertSocialLinksForUserInternal(
        userId: UUID,
        user: User,
        now: Instant,
    ) {
        if (!user.canExposeSocialLinks) {
            SocialLinkTable.deleteWhere { SocialLinkTable.userId eq EntityID(userId, UserTable) }
            return
        }

        SocialLinkTable.deleteWhere { SocialLinkTable.userId eq EntityID(userId, UserTable) }

        user.socialLinks.forEach { link ->
            val entity = link.toEntity(
                userId = userId,
                platformId = link.platform.id,
                createdAt = now,
            )

            SocialLinkTable.insert { row ->
                row[SocialLinkTable.id] = entity.id
                row[SocialLinkTable.userId] = EntityID(entity.userId, UserTable)
                row[platformId] = EntityID(entity.platformId, SocialPlatformTable)
                row[username] = entity.username
                row[completeUrl] = entity.completeUrl
                row[createdAt] = entity.createdAt
            }
        }
    }


}


