package com.simbiri.data.database.entity.therapy

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Stores metadata for S3 objects used by therapy content.
 *
 * Every asset belongs to a TherapySession.
 *
 * Module assets additionally reference TherapyModuleTable.
 * Session-level assets, such as the cover, have therapyModuleId = null.
 */
object TherapyAssetTable : UUIDTable(
    name = "therapy_assets",
) {
    val therapySessionId = uuid("therapy_session_id").references(TherapySessionTable.id).index()

    val therapyModuleId = uuid("therapy_module_id").references(TherapyModuleTable.id).nullable().index()

    val role = varchar("role", 32)

    val mediaType = varchar("media_type", 16)

    val storageKey = varchar("storage_key", 1_024)

    val mimeType = varchar("mime_type", 255)

    val sizeBytes = long("size_bytes")

    val sha256 = varchar("sha256", 64)

    val locale = varchar("locale", 35).nullable()

    val altText = text("alt_text").nullable()

    val transcript = text("transcript").nullable()

    val createdAt = timestamp("created_at")

    val updatedAt = timestamp("updated_at")
}
