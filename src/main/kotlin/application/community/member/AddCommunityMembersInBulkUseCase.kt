package com.simbiri.application.community.member

import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.community.CommunityMemberAssignment
import com.simbiri.domain.policy.community.CommunityMembershipPolicy
import com.simbiri.domain.repository.CommunityRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class AddCommunityMembersInBulkUseCase(
    private val communityRepository: CommunityRepository,
) {

    suspend operator fun invoke(
        communityId: CommunityId,
        assignments: List<CommunityMemberAssignment>,
    ): ResultType<Unit, DataError> {
        if (assignments.isEmpty()) {
            return ResultType.Success(Unit)
        }

        CommunityMembershipPolicy
            .validateBulkAssignments(assignments)
            ?.let { validationError ->
                return ResultType.Failure(validationError)
            }

        return communityRepository.addMembers(
            communityId = communityId,
            assignments = assignments,
        )
    }
}