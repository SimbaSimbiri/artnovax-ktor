package com.simbiri.application.community

import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.repository.CommunityRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class DeleteCommunityUseCase(
    private val communityRepository: CommunityRepository,
) {

    suspend operator fun invoke(
        communityId: CommunityId,
    ): ResultType<Unit, DataError> =
        communityRepository.deleteCommunityById(communityId)
}