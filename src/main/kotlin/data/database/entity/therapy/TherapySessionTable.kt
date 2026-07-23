package com.simbiri.data.database.entity.therapy

import com.simbiri.data.database.entity.user.UserTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Stores one immutable-version candidate of authored therapy content.
 *
 * Only DRAFT content is editable. Once published, a later revision
 * becomes a new row with the same seriesId and a higher version.
 */
object TherapySessionTable : UUIDTable(
    name = "therapy_sessions",
) {
    val seriesId = uuid("series_id").index()

    val authorId = uuid("author_id").references(UserTable.id).index()

    val title = varchar("title", 150)

    val description = text("description")

    val tagline = varchar("tagline", 255).nullable()

    val status = varchar("status", 32).index()

    val version = integer("version")

    val therapeuticPriority = varchar("therapeutic_priority", 32)

    val intensity = varchar("intensity", 32).index()

    val locale = varchar("locale", 35).index()

    val createdAt = timestamp("created_at")

    val updatedAt = timestamp("updated_at")

    val publishedAt = timestamp("published_at").nullable()

    val archivedAt = timestamp("archived_at").nullable()

    init {
        uniqueIndex(
            "uq_therapy_sessions_series_version",
            seriesId,
            version,
        )
    }
}
