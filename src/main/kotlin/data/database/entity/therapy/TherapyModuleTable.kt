package com.simbiri.data.database.entity.therapy

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Stores ordered executable steps owned by a TherapySession.
 */
object TherapyModuleTable : UUIDTable(
    name = "therapy_modules",
) {
    val therapySessionId = uuid("therapy_session_id").references(TherapySessionTable.id).index()

    val orderIndex = integer("order_index")

    val title = varchar("title", 150)

    val goal = varchar("goal", 500)

    val instructions = text("instructions")

    val whyThisHelps = text("why_this_helps")

    val modality = varchar("modality", 32)

    val estimatedDurationSeconds = integer("estimated_duration_seconds")

    val isSkippable = bool("is_skippable").default(false)

    val isRepeatable = bool("is_repeatable").default(true)

    val createdAt = timestamp("created_at")

    val updatedAt = timestamp("updated_at")

    init {
        uniqueIndex(
            "uq_therapy_modules_session_order",
            therapySessionId,
            orderIndex,
        )
    }
}
