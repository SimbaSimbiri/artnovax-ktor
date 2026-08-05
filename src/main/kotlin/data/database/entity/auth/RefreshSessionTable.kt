package com.simbiri.data.database.entity.auth

import com.simbiri.data.database.entity.user.UserTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Stores hashed renewable authentication sessions.
 *
 * Plaintext refresh tokens must never be stored in this table.
 */
object RefreshSessionTable : UUIDTable("refresh_sessions") {

    val userId = uuid("user_id").references(
            ref = UserTable.id,
            onDelete = ReferenceOption.CASCADE,
        ).index(
            customIndexName = "idx_refresh_sessions_user_id"
        )

    val familyId = uuid("family_id").index(
            customIndexName = "idx_refresh_sessions_family_id"
        )

    val tokenHash = varchar(
        name = "token_hash",
        length = 64,
    ).uniqueIndex(
        customIndexName = "uq_refresh_sessions_token_hash"
    )

    val sessionVersion = long("session_version")

    val expiresAt = timestamp(
        "expires_at"
    ).index(
        customIndexName = "idx_refresh_sessions_expires_at"
    )

    val revokedAt = timestamp("revoked_at").nullable()

    val createdAt = timestamp("created_at")

    val updatedAt = timestamp("updated_at")
}
