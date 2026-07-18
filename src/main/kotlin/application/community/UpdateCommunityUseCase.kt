package com.simbiri.application.community

import com.simbiri.domain.model.community.Community
import com.simbiri.domain.policy.community.CommunityPolicy
import com.simbiri.domain.repository.CommunityRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class UpdateCommunityUseCase(
    private val communityRepository: CommunityRepository,
) {

    suspend operator fun invoke(
        community: Community,
    ): ResultType<Unit, DataError> {
        if (community.id == null) {
            return ResultType.Failure(
                DataError.ValidationError(
                    message = "Community update failed. " +
                            "An existing community ID is required."
                )
            )
        }

        CommunityPolicy
            .validateForUpsert(community)
            ?.let { validationError ->
                return ResultType.Failure(validationError)
            }

        return communityRepository.updateCommunity(community)
    }
}