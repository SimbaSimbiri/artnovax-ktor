package com.simbiri.application.therapy.module

import com.simbiri.application.therapy.context.TherapyContentContextLoader
import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.therapy.TherapyModule
import com.simbiri.domain.policy.therapy.TherapyContentAccessPolicy
import com.simbiri.domain.policy.therapy.TherapyContentLifecyclePolicy
import com.simbiri.domain.policy.therapy.TherapyContentPolicy
import com.simbiri.domain.repository.TherapyContentRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class ReorderTherapyModulesUseCase(
    private val therapyContentRepository: TherapyContentRepository,
    private val contextLoader: TherapyContentContextLoader,
) {

    suspend operator fun invoke(
        actorId: UserId,
        therapySessionId: TherapySessionId,
        orderedModuleIds: List<TherapyModuleId>,
    ): ResultType<Unit, DataError> {
        val duplicateId =
            orderedModuleIds
                .groupingBy { moduleId -> moduleId }
                .eachCount()
                .entries
                .firstOrNull { (_, count) ->
                    count > 1
                }
                ?.key

        if (duplicateId != null) {
            return ResultType.Failure(
                DataError.ValidationError(
                    message = "Therapy module reordering failed. " +
                            "orderedModuleIds contains duplicate ID " +
                            "${duplicateId.value}."
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
                operation = "reorder therapy modules",
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

        val persistedModulesById =
            mutableMapOf<TherapyModuleId, TherapyModule>()

        context.session.modules
            .forEachIndexed { index, module ->
                val moduleId =
                    module.id
                        ?: return ResultType.Failure(
                            DataError.DatabaseError(
                                operation =
                                    "reorderTherapyModules",
                                cause = "Persisted TherapyModule is missing its ID. therapySessionId=" +
                                        "${therapySessionId.value}, moduleIndex=$index."
                            )
                        )

                persistedModulesById[moduleId] =
                    module
            }

        val persistedIds =
            persistedModulesById.keys

        val requestedIds =
            orderedModuleIds.toSet()

        if (
            (persistedIds.size != orderedModuleIds.size) || (persistedIds != requestedIds)
        ) {
            return ResultType.Failure(
                DataError.Conflict(
                    message = "Therapy module reordering failed. The supplied order must contain every existing " +
                            "module exactly once. " +
                            "therapySessionId=${therapySessionId.value}, " +
                            "persistedIds=${persistedIds.map { it.value }}, " +
                            "requestedIds=${orderedModuleIds.map { it.value }}, " +
                            "missingIds=${(persistedIds - requestedIds).map { it.value }}, " +
                            "unexpectedIds=${(requestedIds - persistedIds).map { it.value }}."
                )
            )
        }

        val reorderedModules =
            orderedModuleIds.mapIndexed { index, moduleId ->
                requireNotNull(
                    persistedModulesById[moduleId]
                ).copy(
                    orderIndex = index
                )
            }

        val candidate =
            context.session.copy(
                modules = reorderedModules
            )

        TherapyContentPolicy
            .validateDraft(candidate)
            ?.let { error ->
                return ResultType.Failure(error)
            }

        return therapyContentRepository.reorderModules(
            therapySessionId = therapySessionId,
            orderedModuleIds = orderedModuleIds,
        )
    }
}