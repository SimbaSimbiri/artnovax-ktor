package com.simbiri.application.therapy.asset

import com.simbiri.application.therapy.context.TherapyContentContextLoader
import com.simbiri.domain.model.common.TherapyAssetId
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.policy.therapy.TherapyContentAccessPolicy
import com.simbiri.domain.repository.TherapyAssetRepository
import com.simbiri.domain.storage.TherapyAssetDownloadGrant
import com.simbiri.domain.storage.TherapyAssetObjectStorage
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class GetManagedTherapyAssetDownloadUseCase(
    private val contextLoader: TherapyContentContextLoader,
    private val therapyAssetRepository: TherapyAssetRepository,
    private val objectStorage: TherapyAssetObjectStorage,
) {

    suspend operator fun invoke(
        actorId: UserId,
        therapySessionId: TherapySessionId,
        therapyAssetId: TherapyAssetId,
    ): ResultType<TherapyAssetDownloadGrant, DataError> {
        val context = when (
            val result = contextLoader.load(
                actorId = actorId,
                therapySessionId = therapySessionId,
            )
        ) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> return ResultType.Failure(result.error)
        }

        TherapyContentAccessPolicy.validateCanViewManagedContent(
            actor = context.actor,
            session = context.session,
        )?.let { error ->
            return ResultType.Failure(error)
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
