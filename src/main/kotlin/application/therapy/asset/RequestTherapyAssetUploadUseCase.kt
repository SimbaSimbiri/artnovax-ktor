package com.simbiri.application.therapy.asset

import com.simbiri.application.therapy.context.TherapyContentContextLoader
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.policy.therapy.TherapyAssetUploadPolicy
import com.simbiri.domain.policy.therapy.TherapyContentAccessPolicy
import com.simbiri.domain.policy.therapy.TherapyContentLifecyclePolicy
import com.simbiri.domain.storage.TherapyAssetUploadGateway
import com.simbiri.domain.storage.TherapyAssetUploadGrant
import com.simbiri.domain.storage.TherapyAssetUploadSpecification
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.util.Locale

class RequestTherapyAssetUploadUseCase(
    private val contextLoader: TherapyContentContextLoader,
    private val uploadGateway: TherapyAssetUploadGateway,
    private val storageKeyFactory: TherapyAssetStorageKeyFactory,
) {

    suspend operator fun invoke(
        actorId: UserId,
        request: TherapyAssetUploadRequest,
    ): ResultType<TherapyAssetUploadGrant, DataError> {
        val context = when (val result = contextLoader.load(
            actorId = actorId,
            therapySessionId = request.therapySessionId,
        )) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> return ResultType.Failure(result.error)
        }

        TherapyContentAccessPolicy.validateCanManageDraft(
            actor = context.actor,
            session = context.session,
            operation = "request therapy asset upload",
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

        TherapyAssetUploadPolicy.validate(request)?.let { error ->
            return ResultType.Failure(error)
        }

        val storageKey = storageKeyFactory.create(
            therapySessionId = request.therapySessionId,
            therapyModuleId = request.therapyModuleId,
        )

        return uploadGateway.createUploadGrant(
            TherapyAssetUploadSpecification(
                storageKey = storageKey,
                mimeType = request.mimeType.trim().lowercase(Locale.ROOT),
                sizeBytes = request.sizeBytes,
                sha256 = request.sha256.trim().lowercase(Locale.ROOT),
            )
        )
    }
}
