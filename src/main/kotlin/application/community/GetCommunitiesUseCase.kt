package com.simbiri.application.community

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.community.Community
import com.simbiri.domain.repository.CommunityRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class GetCommunitiesUseCase(
    private val communityRepository: CommunityRepository,
) {

    suspend operator fun invoke(
        approved: Boolean? = null,
        ownerId: UserId? = null,
    ): ResultType<List<Community>, DataError> =
        communityRepository.getCommunities(
            approved = approved,
            ownerId = ownerId,
        )
}