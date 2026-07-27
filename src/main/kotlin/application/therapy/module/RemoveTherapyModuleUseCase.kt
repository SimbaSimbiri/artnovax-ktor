package com.simbiri.application.therapy.module

import com.simbiri.application.therapy.context.TherapyContentContextLoader
import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.policy.therapy.TherapyContentAccessPolicy
import com.simbiri.domain.policy.therapy.TherapyContentLifecyclePolicy
import com.simbiri.domain.policy.therapy.TherapyContentPolicy
import com.simbiri.domain.repository.TherapyContentRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class RemoveTherapyModuleUseCase(
    private val therapyContentRepository: TherapyContentRepository,
    private val contextLoader: TherapyContentContextLoader,
) {

    suspend operator fun invoke(
        actorId: UserId,
        therapySessionId: TherapySessionId,
        therapyModuleId: TherapyModuleId,
    ): ResultType<Unit, DataError> {
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
                operation = "remove therapy module",
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

        val moduleExists =
            context.session.modules.any { module ->
                module.id == therapyModuleId
            }

        if (!moduleExists) {
            return ResultType.Failure(
                DataError.NotFound
            )
        }

        val remainingModules =
            context.session.modules
                .filterNot { module ->
                    module.id == therapyModuleId
                }
                .sortedBy { module ->
                    module.orderIndex
                }
                .mapIndexed { index, module ->
                    module.copy(
                        orderIndex = index
                    )
                }

        val candidate =
            context.session.copy(
                modules = remainingModules
            )

        TherapyContentPolicy
            .validateDraft(candidate)
            ?.let { error ->
                return ResultType.Failure(error)
            }

        return therapyContentRepository.removeModule(
            therapySessionId = therapySessionId,
            therapyModuleId = therapyModuleId,
        )
    }
}
