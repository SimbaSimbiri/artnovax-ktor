package com.simbiri.data.database.entity.therapy

import org.jetbrains.exposed.sql.Table

object TherapySessionContraindicationTable : Table(
    name = "therapy_session_contraindications",
) {
    val therapySessionId = uuid("therapy_session_id").references(TherapySessionTable.id)

    val contraindication = varchar("contraindication", 64)

    override val primaryKey = PrimaryKey(
        therapySessionId,
        contraindication,
        name = "pk_therapy_session_contraindications",
    )
}
