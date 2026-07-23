package com.simbiri.data.database.entity.therapy

import org.jetbrains.exposed.sql.Table

object TherapySessionGoalTable : Table(
    name = "therapy_session_goals",
) {
    val therapySessionId = uuid("therapy_session_id").references(TherapySessionTable.id)

    val goal = varchar("goal", 64)

    override val primaryKey = PrimaryKey(
        therapySessionId,
        goal,
        name = "pk_therapy_session_goals",
    )
}
