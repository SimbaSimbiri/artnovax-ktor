package com.simbiri.data.database.entity.therapy

import org.jetbrains.exposed.sql.Table

object TherapySessionCultureTagTable : Table(
    name = "therapy_session_culture_tags",
) {
    val therapySessionId = uuid("therapy_session_id").references(TherapySessionTable.id)

    /**
     * Original display value.
     */
    val tag = varchar("tag", 80)

    /**
     * Trimmed lowercase value used for uniqueness.
     */
    val normalizedTag = varchar("normalized_tag", 80)

    override val primaryKey = PrimaryKey(
        therapySessionId,
        normalizedTag,
        name = "pk_therapy_session_culture_tags",
    )
}
