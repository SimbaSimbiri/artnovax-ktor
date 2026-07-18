package com.simbiri.application.community

import com.simbiri.domain.model.community.Community
import com.simbiri.domain.policy.community.CommunityPolicy
import com.simbiri.domain.repository.CommunityRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class CreateCommunitiesInBulkUseCase(
    private val communityRepository: CommunityRepository,
) {

    suspend operator fun invoke(
        communities: List<Community>,
    ): ResultType<Unit, DataError> {
        if (communities.isEmpty()) {
            return ResultType.Success(Unit)
        }

        communities.forEachIndexed { index, community ->
            if (community.id != null) {
                return ResultType.Failure(
                    bulkValidationError(
                        index = index,
                        community = community,
                        reason = "Bulk community creation only accepts " +
                                "communities without existing IDs."
                    )
                )
            }

            if (community.memberCount != 0) {
                return ResultType.Failure(
                    bulkValidationError(
                        index = index,
                        community = community,
                        reason = "memberCount is server-managed and must be " +
                                "zero for new communities. " +
                                "receivedMemberCount=${community.memberCount}."
                    )
                )
            }

            CommunityPolicy
                .validateForUpsert(community)
                ?.let { validationError ->
                    return ResultType.Failure(
                        DataError.ValidationError(
                            message = "Bulk community creation failed at " +
                                    "index=$index. " +
                                    "community.name=${community.name}, " +
                                    "ownerId=${community.ownerId.value}. " +
                                    "Nested error: ${validationError.message}"
                        )
                    )
                }
        }

        return communityRepository.createCommunities(communities)
    }

    private fun bulkValidationError(
        index: Int,
        community: Community,
        reason: String,
    ): DataError.ValidationError =
        DataError.ValidationError(
            message = "Bulk community creation failed at index=$index. " +
                    "community.id=${community.id}, " +
                    "community.name=${community.name}, " +
                    "ownerId=${community.ownerId.value}. $reason"
        )
}