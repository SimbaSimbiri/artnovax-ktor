package com.simbiri.application.therapy.asset

import com.simbiri.application.therapy.context.TherapyContentContextLoader
import com.simbiri.domain.model.common.TherapyAssetId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.policy.therapy.TherapyAssetObjectPolicy
import com.simbiri.domain.policy.therapy.TherapyAssetStorageKeyPolicy
import com.simbiri.domain.policy.therapy.TherapyAssetUploadPolicy
import com.simbiri.domain.policy.therapy.TherapyContentAccessPolicy
import com.simbiri.domain.policy.therapy.TherapyContentLifecyclePolicy
import com.simbiri.domain.repository.TherapyAssetRepository
import com.simbiri.domain.storage.TherapyAssetObjectStorage
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import org.slf4j.LoggerFactory

class ConfirmTherapyAssetUploadUseCase(
    private val contextLoader: TherapyContentContextLoader,
    private val therapyAssetRepository: TherapyAssetRepository,
    private val objectStorage: TherapyAssetObjectStorage,
) {

    suspend operator fun invoke(
        actorId: UserId,
        request: ConfirmTherapyAssetUploadRequest,
    ): ResultType<TherapyAssetId, DataError> {
        val context = when (
            val result = contextLoader.load(
                actorId = actorId,
                therapySessionId = request.therapySessionId,
            )
        ) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> return ResultType.Failure(result.error)
        }

        TherapyContentAccessPolicy.validateCanManageDraft(
            actor = context.actor,
            session = context.session,
            operation = "confirm therapy asset upload",
        )?.let { error ->
            return ResultType.Failure(error)
        }

        TherapyContentLifecyclePolicy.validateContentMutationAllowed(context.session)?.let { error ->
            return ResultType.Failure(error)
        }

        request.therapyModuleId?.let { therapyModuleId ->
            val moduleExists = context.session.modules.any { module ->
                module.id == therapyModuleId
            }

            if (!moduleExists) {
                return ResultType.Failure(DataError.NotFound)
            }
        }

        TherapyAssetUploadPolicy.validate(request.toUploadValidationRequest())?.let { error ->
            return ResultType.Failure(error)
        }

        TherapyAssetStorageKeyPolicy.validate(
            therapySessionId = request.therapySessionId,
            therapyModuleId = request.therapyModuleId,
            storageKey = request.storageKey,
        )?.let { error ->
            return ResultType.Failure(error)
        }

        val storedObject = when (
            val result = objectStorage.inspectObject(request.storageKey)
        ) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> return ResultType.Failure(result.error)
        }

        TherapyAssetObjectPolicy.validate(
            request = request,
            storedObject = storedObject,
        )?.let { error ->
            return ResultType.Failure(error)
        }

        val replacement = when (
            val result = therapyAssetRepository.replaceAsset(
                therapySessionId = request.therapySessionId,
                therapyModuleId = request.therapyModuleId,
                asset = request.toTherapyAsset(),
            )
        ) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> return ResultType.Failure(result.error)
        }

        replacement.replacedStorageKeys
            .filterNot { storageKey ->
                storageKey == request.storageKey
            }
            .forEach { storageKey ->
                when (val deletion = objectStorage.deleteObject(storageKey)) {
                    is ResultType.Success -> Unit

                    is ResultType.Failure -> {
                        logger.warn(
                            "Unable to remove replaced therapy asset. storageKey={}, error={}",
                            storageKey,
                            deletion.error,
                        )
                    }
                }
            }

        return ResultType.Success(replacement.therapyAssetId)
    }

    private companion object {
        val logger = LoggerFactory.getLogger(ConfirmTherapyAssetUploadUseCase::class.java)
    }
}
