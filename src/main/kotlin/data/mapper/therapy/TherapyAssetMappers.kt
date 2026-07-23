package com.simbiri.data.mapper.therapy

import com.simbiri.data.database.entity.therapy.TherapyAssetEntity
import com.simbiri.domain.model.common.TherapyAssetId
import com.simbiri.domain.model.therapy.TherapyAsset
import com.simbiri.domain.model.therapy.TherapyAssetRole
import com.simbiri.domain.model.therapy.TherapyMediaType
import java.time.Instant
import java.util.*

fun TherapyAssetEntity.toDomain(): TherapyAsset = TherapyAsset(
    id = TherapyAssetId(id),
    role = TherapyAssetRole.valueOf(roleName),
    mediaType = TherapyMediaType.valueOf(mediaTypeName),
    storageKey = storageKey,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    sha256 = sha256,
    locale = locale,
    altText = altText,
    transcript = transcript,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun TherapyAsset.toEntity(
    therapySessionId: UUID,
    therapyModuleId: UUID?,
    now: Instant,
): TherapyAssetEntity = TherapyAssetEntity(
    id = id?.value ?: UUID.randomUUID(),
    therapySessionId = therapySessionId,
    therapyModuleId = therapyModuleId,
    roleName = role.name,
    mediaTypeName = mediaType.name,
    storageKey = storageKey.trim(),
    mimeType = mimeType.trim(),
    sizeBytes = sizeBytes,
    sha256 = sha256.trim().lowercase(),
    locale = locale?.trim(),
    altText = altText?.trim(),
    transcript = transcript?.trim(),
    createdAt = createdAt ?: now,
    updatedAt = now,
)
