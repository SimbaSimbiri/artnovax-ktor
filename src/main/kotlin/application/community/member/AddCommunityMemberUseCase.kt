package com.simbiri.application.community.member


import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.community.CommunityParticipantRole
import com.simbiri.domain.repository.CommunityRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class AddCommunityMemberUseCase(
    private val communityRepository: CommunityRepository,
) {

    suspend operator fun invoke(
        communityId: CommunityId,
        userId: UserId,
        role: CommunityParticipantRole,
    ): ResultType<Unit, DataError> =
        communityRepository.addMember(
            communityId = communityId,
            userId = userId,
            role = role,
        )
}