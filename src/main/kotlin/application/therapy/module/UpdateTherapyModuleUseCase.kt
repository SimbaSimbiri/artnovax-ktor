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

        val context = when (val result = contextLoader.load(
            actorId = actorId,
            therapySessionId = therapySessionId,
        )) {
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

        TherapyContentLifecyclePolicy.validateContentMutationAllowed(
                context.session
            )?.let { error ->
                return ResultType.Failure(error)
            }

        val persistedModule = context.session.modules.firstOrNull { existing ->
                existing.id == therapyModuleId
            } ?: return ResultType.Failure(
            DataError.NotFound
        )

        if (module.orderIndex != persistedModule.orderIndex) {
            return ResultType.Failure(
                DataError.Conflict(
                    message = "Therapy module update failed. Module ordering cannot be changed through "
                            + "UpdateTherapyModuleUseCase. therapyModuleId=${therapyModuleId.value}, "
                            + "persistedOrderIndex=${persistedModule.orderIndex}, requestedOrderIndex=${module.orderIndex}. "
                            + "Use ReorderTherapyModulesUseCase."
                )
            )
        }

        TherapyModulePolicy.validateDraft(module)?.let { error ->
                return ResultType.Failure(error)
            }

        val candidate = context.session.copy(
            modules = context.session.modules.map { existing ->
                if (existing.id == therapyModuleId) {
                    module
                } else {
                    existing
                }
            })

        TherapyContentPolicy.validateDraft(candidate)?.let { error ->
                return ResultType.Failure(error)
            }

        return therapyContentRepository.updateModule(
            therapySessionId = therapySessionId,
            module = module,
        )
    }
}
