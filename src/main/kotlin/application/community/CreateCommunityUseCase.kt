package com.simbiri.application.community

import com.simbiri.domain.model.community.Community
import com.simbiri.domain.policy.community.CommunityPolicy
import com.simbiri.domain.repository.CommunityRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class CreateCommunityUseCase(private val communityRepository: CommunityRepository) {
    suspend operator fun invoke(community: Community, ): ResultType<Unit, DataError>{
        if (community.id != null) {
            return ResultType.Failure(
                DataError.ValidationError(
                    message = "Community creation failed. " +
                            "A new community must not already have an ID. " +
                            "receivedCommunityId=${community.id.value}."
                )
            )
        }

        if (community.memberCount != 0) {
            return ResultType.Failure(
                DataError.ValidationError(
                    message = "Community creation failed. " +
                            "memberCount is server-managed and must be zero " +
                            "for a new community. " +
                            "receivedMemberCount=${community.memberCount}."
                )
            )
        }

        CommunityPolicy
            .validateForUpsert(community)
            ?.let { validationError ->
                return ResultType.Failure(validationError)
            }

        return communityRepository.createCommunity(community)
    }
}