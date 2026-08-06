package com.simbiri.application.therapy.asset

import com.simbiri.domain.model.common.TherapyAssetId
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.therapy.TherapyContentStatus
import com.simbiri.domain.repository.TherapyAssetRepository
import com.simbiri.domain.repository.TherapyContentRepository
import com.simbiri.domain.storage.TherapyAssetDownloadGrant
import com.simbiri.domain.storage.TherapyAssetObjectStorage
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class GetPublishedTherapyAssetDownloadUseCase(
    private val therapyContentRepository: TherapyContentRepository,
    private val therapyAssetRepository: TherapyAssetRepository,
    private val objectStorage: TherapyAssetObjectStorage,
) {

    suspend operator fun invoke(
        therapySessionId: TherapySessionId,
        therapyAssetId: TherapyAssetId,
    ): ResultType<TherapyAssetDownloadGrant, DataError> {
        val session = when (
            val result = therapyContentRepository.getTherapySessionById(therapySessionId)
        ) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> return ResultType.Failure(result.error)
        }

        if (session.status != TherapyContentStatus.PUBLISHED) {
            return ResultType.Failure(DataError.NotFound)
        }

        val asset = when (
            val result = therapyAssetRepository.getAsset(
                therapySessionId = therapySessionId,
                therapyAssetId = therapyAssetId,
            )
        ) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> return ResultType.Failure(result.error)
        }

        return objectStorage.createDownloadGrant(asset.storageKey)
    }
}
