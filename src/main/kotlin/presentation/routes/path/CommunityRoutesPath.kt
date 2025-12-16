package com.simbiri.presentation.routes.path

import io.ktor.resources.*

@Resource("/communities")
class CommunityRoutesPath(
    val approved: Boolean? = null,
    val ownerId: String? = null,
) {

    @Resource("{communityId}")
    data class ById(
        val parent: CommunityRoutesPath = CommunityRoutesPath(),
        val communityId: String,
    )

    @Resource("bulk")
    data class Bulk(
        val parent: CommunityRoutesPath = CommunityRoutesPath(),
    )

    @Resource("{communityId}/members")
    data class Members(
        val parent: CommunityRoutesPath = CommunityRoutesPath(),
        val communityId: String,
    ){
        @Resource("{userId}")
        data class MemberById(
            val parent: Members,
            val userId: String,
        )

        @Resource("bulk")
        data class Bulk(
            val parent: Members,
        )
    }


}