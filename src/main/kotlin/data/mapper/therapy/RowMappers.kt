package com.simbiri.data.mapper.therapy

import com.simbiri.data.database.entity.therapy.TherapyAssetEntity
import com.simbiri.data.database.entity.therapy.TherapyAssetTable
import com.simbiri.data.database.entity.therapy.TherapyModuleEntity
import com.simbiri.data.database.entity.therapy.TherapyModuleTable
import com.simbiri.data.database.entity.therapy.TherapySessionEntity
import com.simbiri.data.database.entity.therapy.TherapySessionTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toTherapySessionEntity(): TherapySessionEntity =
    TherapySessionEntity(
        id = this[TherapySessionTable.id].value,
        seriesId = this[TherapySessionTable.seriesId],
        authorId = this[TherapySessionTable.authorId],
        title = this[TherapySessionTable.title],
        description = this[TherapySessionTable.description],
        tagline = this[TherapySessionTable.tagline],
        statusName = this[TherapySessionTable.status],
        version = this[TherapySessionTable.version],
        therapeuticPriorityName =
            this[TherapySessionTable.therapeuticPriority],
        intensityName = this[TherapySessionTable.intensity],
        locale = this[TherapySessionTable.locale],
        createdAt = this[TherapySessionTable.createdAt],
        updatedAt = this[TherapySessionTable.updatedAt],
        publishedAt = this[TherapySessionTable.publishedAt],
        archivedAt = this[TherapySessionTable.archivedAt],
    )

fun ResultRow.toTherapyModuleEntity(): TherapyModuleEntity =
    TherapyModuleEntity(
        id = this[TherapyModuleTable.id].value,
        therapySessionId =
            this[TherapyModuleTable.therapySessionId],
        orderIndex = this[TherapyModuleTable.orderIndex],
        title = this[TherapyModuleTable.title],
        goal = this[TherapyModuleTable.goal],
        instructions = this[TherapyModuleTable.instructions],
        whyThisHelps =
            this[TherapyModuleTable.whyThisHelps],
        modalityName = this[TherapyModuleTable.modality],
        estimatedDurationSeconds =
            this[
                TherapyModuleTable.estimatedDurationSeconds
            ],
        isSkippable =
            this[TherapyModuleTable.isSkippable],
        isRepeatable =
            this[TherapyModuleTable.isRepeatable],
        createdAt = this[TherapyModuleTable.createdAt],
        updatedAt = this[TherapyModuleTable.updatedAt],
    )

fun ResultRow.toTherapyAssetEntity(): TherapyAssetEntity =
    TherapyAssetEntity(
        id = this[TherapyAssetTable.id].value,
        therapySessionId =
            this[TherapyAssetTable.therapySessionId],
        therapyModuleId =
            this[TherapyAssetTable.therapyModuleId],
        roleName = this[TherapyAssetTable.role],
        mediaTypeName = this[TherapyAssetTable.mediaType],
        storageKey = this[TherapyAssetTable.storageKey],
        mimeType = this[TherapyAssetTable.mimeType],
        sizeBytes = this[TherapyAssetTable.sizeBytes],
        sha256 = this[TherapyAssetTable.sha256],
        locale = this[TherapyAssetTable.locale],
        altText = this[TherapyAssetTable.altText],
        transcript = this[TherapyAssetTable.transcript],
        createdAt = this[TherapyAssetTable.createdAt],
        updatedAt = this[TherapyAssetTable.updatedAt],
    )
