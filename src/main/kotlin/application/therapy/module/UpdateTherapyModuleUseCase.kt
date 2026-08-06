package com.simbiri.application.therapy.module

import com.simbiri.application.therapy.context.TherapyContentContextLoader
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.therapy.TherapyModule
import com.simbiri.domain.policy.therapy.TherapyContentAccessPolicy
import com.simbiri.domain.policy.therapy.TherapyContentLifecyclePolicy
import com.simbiri.domain.policy.therapy.TherapyContentPolicy
import com.simbiri.domain.policy.therapy.TherapyModulePolicy
import com.simbiri.domain.repository.TherapyContentRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class UpdateTherapyModuleUseCase(
    private val therapyContentRepository: TherapyContentRepository,
    private val contextLoader: TherapyContentContextLoader,
) {

    suspend operator fun invoke(
        actorId: UserId,
        therapySessionId: TherapySessionId,
        module: TherapyModule,
    ): ResultType<Unit, DataError> {
        val therapyModuleId = module.id ?: return ResultType.Failure(
            DataError.ValidationError(
                message = "Therapy module update failed. A persisted therapy-module ID is required."
            )
        )

        val context = when (
            val result = contextLoader.load(
                actorId = actorId,
                therapySessionId = therapySessionId,
            )
        ) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> return ResultType.Failure(result.error)
        }

        TherapyContentAccessPolicy.validateCanManageDraft(
            actor = context.actor,
            session = context.session,
            operation = "update therapy module",
        )?.let { error ->
            return ResultType.Failure(error)
        }

        TherapyContentLifecyclePolicy.validateContentMutationAllowed(context.session)?.let { error ->
            return ResultType.Failure(error)
        }

        val persistedModule = context.session.modules.firstOrNull { existing ->
            existing.id == therapyModuleId
        } ?: return ResultType.Failure(DataError.NotFound)

        /*
         * Only module metadata is writable through this operation. Ordering, assets, identity, and persistence
         * timestamps remain server-owned.
         */
        val candidateModule = persistedModule.copy(
            title = module.title,
            goal = module.goal,
            instructions = module.instructions,
            whyThisHelps = module.whyThisHelps,
            modality = module.modality,
            estimatedDurationSeconds = module.estimatedDurationSeconds,
            isSkippable = module.isSkippable,
            isRepeatable = module.isRepeatable,
        )

        TherapyModulePolicy.validateDraft(candidateModule)?.let { error ->
            return ResultType.Failure(error)
        }

        val candidateSession = context.session.copy(
            modules = context.session.modules.map { existing ->
                if (existing.id == therapyModuleId) candidateModule else existing
            }
        )

        TherapyContentPolicy.validateDraft(candidateSession)?.let { error ->
            return ResultType.Failure(error)
        }

        return therapyContentRepository.updateModule(
            therapySessionId = therapySessionId,
            module = candidateModule,
        )
    }
}
