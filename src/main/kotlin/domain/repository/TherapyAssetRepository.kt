package com.simbiri.domain.repository

import com.simbiri.domain.model.common.TherapyAssetId
import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.therapy.TherapyAsset
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

interface TherapyAssetRepository {

    suspend fun replaceAsset(
        therapySessionId: TherapySessionId,
        therapyModuleId: TherapyModuleId?,
        asset: TherapyAsset,
    ): ResultType<TherapyAssetReplacementResult, DataError>

    suspend fun getAsset(
        therapySessionId: TherapySessionId,
        therapyAssetId: TherapyAssetId,
    ): ResultType<TherapyAsset, DataError>
}

data class TherapyAssetReplacementResult(
    val therapyAssetId: TherapyAssetId,
    val replacedStorageKeys: Set<String>,
)
