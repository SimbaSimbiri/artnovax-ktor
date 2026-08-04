package com.simbiri.data.repository

import com.simbiri.data.database.entity.auth.AuthenticationCredentialTable
import com.simbiri.data.database.entity.user.UserTable
import com.simbiri.data.database.utils.dbQuery
import com.simbiri.data.repository.util.databaseError
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.repository.AccessTokenSessionRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll

/**
 * Validates persisted access-token state with one joined database query.
 */
class AccessTokenSessionRepoImpl(
    private val db: Database,
) : AccessTokenSessionRepository {

    override suspend fun isCurrent(
        userId: UserId,
        sessionVersion: Long,
    ): ResultType<Boolean, DataError> {
        val operation = "isAccessTokenSessionCurrent"

        if (sessionVersion <= 0L) {
            return ResultType.Success(false)
        }

        return try {
            val isCurrent = db.dbQuery {
                (UserTable innerJoin AuthenticationCredentialTable).selectAll().where {
                        (UserTable.id eq EntityID(
                            id = userId.value,
                            table = UserTable,
                        )) and (UserTable.isActive eq true) and (AuthenticationCredentialTable.sessionVersion eq sessionVersion)
                    }.limit(1).any()
            }

            ResultType.Success(
                isCurrent
            )
        } catch (e: Exception) {
            ResultType.Failure(
                databaseError(
                    operation = operation,
                    e = e,
                    details = "userId=${userId.value}",
                )
            )
        }
    }
}
