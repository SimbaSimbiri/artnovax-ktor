package com.simbiri.data.database.entity.auth

import com.simbiri.data.database.entity.user.UserTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Stores one authentication credential per persisted User.
 *
 * The User foreign key is also the primary key, enforcing the one-to-one
 * relationship.
 */
object AuthenticationCredentialTable : Table("authentication_credentials") {

    val userId = uuid("user_id").references(
            UserTable.id,
            onDelete = ReferenceOption.CASCADE,
        )

    val passwordHash = varchar(
        name = "password_hash",
        length = 512,
    )

    val passwordAlgorithm = varchar(
        name = "password_algorithm",
        length = 32,
    )

    val passwordUpdatedAt = timestamp("password_updated_at")

    val failedLoginAttempts = integer("failed_login_attempts").default(0)

    val lockedUntil = timestamp("locked_until").nullable()

    val createdAt = timestamp("created_at")

    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(
        userId,
        name = "pk_authentication_credentials",
    )
}
