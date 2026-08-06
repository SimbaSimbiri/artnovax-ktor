package com.simbiri.domain.storage

import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.time.Instant

interface TherapyAssetObjectStorage {

    suspend fun inspectObject(
        storageKey: String,
    ): ResultType<TherapyStoredObject, DataError>

    suspend fun createDownloadGrant(
        storageKey: String,
    ): ResultType<TherapyAssetDownloadGrant, DataError>

    suspend fun deleteObject(
        storageKey: String,
    ): ResultType<Unit, DataError>
}

data class TherapyStoredObject(
    val storageKey: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val sha256: String?,
)

data class TherapyAssetDownloadGrant(
    val downloadUrl: String,
    val expiresAt: Instant,
)
