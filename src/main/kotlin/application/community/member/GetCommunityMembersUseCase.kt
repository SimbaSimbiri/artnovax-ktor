package com.simbiri.application.community.member

import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.community.CommunityMember
import com.simbiri.domain.repository.CommunityRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class GetCommunityMembersUseCase(
    private val communityRepository: CommunityRepository,
) {

    suspend operator fun invoke(
        communityId: CommunityId,
    ): ResultType<List<CommunityMember>, DataError> =
        communityRepository.getMembers(communityId)
}