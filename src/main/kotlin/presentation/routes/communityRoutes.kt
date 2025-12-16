package com.simbiri.presentation.routes

import com.simbiri.domain.model.common.CommunityId
import com.simbiri.domain.repository.CommunityRepository
import com.simbiri.domain.util.onFailure
import com.simbiri.domain.util.onSuccess
import com.simbiri.presentation.routes.dto.community.*
import com.simbiri.presentation.routes.path.CommunityRoutesPath
import com.simbiri.presentation.utils.respondWithDataError
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.Routing
import java.util.UUID

fun Routing.communityRoutes(
    communityRepository: CommunityRepository,
) {

    // GET /communities
    get<CommunityRoutesPath> { path ->
        communityRepository
            .getAllCommunities(
                approved = path.approved,
                ownerId = path.ownerId,
            )
            .onSuccess { communities ->
                call.respond(
                    status = HttpStatusCode.OK,
                    message = communities.toCommResponseDto(),
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // GET /communities/{communityId}
    get<CommunityRoutesPath.ById> { path ->
        communityRepository
            .getCommunityById(path.communityId)
            .onSuccess { community ->
                call.respond(
                    status = HttpStatusCode.OK,
                    message = community.toCommResponseDto(),
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // POST /communities (create)
    post<CommunityRoutesPath> {
        val dto = call.receive<CommunityUpsertDto>()
        val domain = dto.toDomain(existingId = null)

        communityRepository
            .upsertCommunity(domain)
            .onSuccess {
                call.respond(
                    status = HttpStatusCode.Created,
                    message = "Community created successfully.",
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // POST /communities/bulk
    post<CommunityRoutesPath.Bulk> {
        val listDto = call.receive<List<CommunityUpsertDto>>()
        val listDomain = listDto.map { it.toDomain(existingId = null) }

        communityRepository.insertCommunitiesInBulk(listDomain)
            .onSuccess {
                call.respond(
                    status = HttpStatusCode.Created,
                    message= "All communities added successfully.",
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // DELETE community by ID
    delete<CommunityRoutesPath.ById>{path->

        communityRepository.deleteCommunityById(path.communityId)
            .onSuccess {
                call.respond(
                    HttpStatusCode.NoContent
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // POST /communities/{communityId} (update via upsert)
    post<CommunityRoutesPath.ById> { path ->
        val dto = call.receive<CommunityUpsertDto>()
        val domain = dto.toDomain(existingId = path.communityId)

        communityRepository
            .upsertCommunity(domain)
            .onSuccess {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = "Community updated successfully.",
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // GET /communities/{communityId}/members
    get<CommunityRoutesPath.Members> { path ->
        communityRepository
            .listMembers(path.communityId)
            .onSuccess { members ->
                call.respond(
                    status = HttpStatusCode.OK,
                    message = members.toMembersResponseDto(),
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // POST /communities/{communityId}/members (upsert membership)
    post<CommunityRoutesPath.Members> { path ->
        val dto = call.receive<CommunityMemberUpsertDto>()
        val role = dto.toRole()

        communityRepository
            .upsertMember(
                communityId = path.communityId,
                userId = dto.userId,
                role = role,
            )
            .onSuccess {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = "Membership updated successfully.",
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    post<CommunityRoutesPath.Members.Bulk> { path ->
        val communityId = path.parent.communityId

        val memberDtos = call.receive<List<CommunityMemberUpsertDto>>()

        val communityIdDomain = CommunityId(UUID.fromString(communityId))
        val membersDomain = memberDtos.map { it.toDomain(communityIdDomain) }

        communityRepository
            .upsertMembersInBulk(communityId = communityId, members = membersDomain)
            .onSuccess {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = "Members upserted successfully for community $communityId.",
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }


    // DELETE /communities/{communityId}/members/{userId}
    delete<CommunityRoutesPath.Members.MemberById> { path ->
        communityRepository
            .removeMember(
                communityId = path.parent.communityId,
                userId = path.userId,
            )
            .onSuccess {
                call.respond(
                 HttpStatusCode.NoContent
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }
}
