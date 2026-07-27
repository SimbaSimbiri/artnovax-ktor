package com.simbiri.application.therapy

import com.simbiri.application.therapy.context.TherapyContentContextLoader
import com.simbiri.domain.model.common.TherapyModuleId
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

class AddTherapyModuleUseCase(
    private val therapyContentRepository: TherapyContentRepository,
    private val contextLoader: TherapyContentContextLoader,
) {

    suspend operator fun invoke(
        actorId: UserId,
        therapySessionId: TherapySessionId,
        module: TherapyModule,
    ): ResultType<TherapyModuleId, DataError> {
        if (module.id != null) {
            return ResultType.Failure(
                DataError.ValidationError(
                    message = "Therapy module creation failed. " +
                            "A new module must not already have an ID. " +
                            "receivedTherapyModuleId=${module.id.value}."
                )
            )
        }

        val context =
            when (
                val result =
                    contextLoader.load(
                        actorId = actorId,
                        therapySessionId = therapySessionId,
                    )
            ) {
                is ResultType.Success ->
                    result.data

                is ResultType.Failure ->
                    return ResultType.Failure(result.error)
            }

        TherapyContentAccessPolicy
            .validateCanManageDraft(
                actor = context.actor,
                session = context.session,
                operation = "add therapy module",
            )
            ?.let { error ->
                return ResultType.Failure(error)
            }

        TherapyContentLifecyclePolicy
            .validateContentMutationAllowed(
                context.session
            )
            ?.let { error ->
                return ResultType.Failure(error)
            }

        TherapyModulePolicy
            .validateDraft(module)
            ?.let { error ->
                return ResultType.Failure(error)
            }

        val currentModuleCount =
            context.session.modules.size

        if (
            module.orderIndex !in
            0..currentModuleCount
        ) {
            return ResultType.Failure(
                DataError.ValidationError(
                    message = "Therapy module creation failed. " +
                            "field=orderIndex, " +
                            "value=${module.orderIndex}. " +
                            "Order index must be between 0 and " +
                            "$currentModuleCount, inclusive."
                )
            )
        }

        val shiftedModules =
            context.session.modules.map { existing ->
                if (
                    existing.orderIndex >=
                    module.orderIndex
                ) {
                    existing.copy(
                        orderIndex =
                            existing.orderIndex + 1
                    )
                } else {
                    existing
                }
            }

        val candidate =
            context.session.copy(
                modules =
                    (shiftedModules + module)
                        .sortedBy { it.orderIndex }
            )

        TherapyContentPolicy
            .validateDraft(candidate)
            ?.let { error ->
                return ResultType.Failure(error)
            }

        return therapyContentRepository.addModule(
            therapySessionId = therapySessionId,
            module = module,
        )
    }
}
