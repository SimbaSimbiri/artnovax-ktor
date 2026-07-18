package com.simbiri.application.community

import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.community.Community
import com.simbiri.domain.repository.CommunityRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class GetCommunityByIdUseCase(
    private val communityRepository: CommunityRepository,
) {

    suspend operator fun invoke(
        communityId: CommunityId,
    ): ResultType<Community, DataError> =
        communityRepository.getCommunityById(communityId)
}