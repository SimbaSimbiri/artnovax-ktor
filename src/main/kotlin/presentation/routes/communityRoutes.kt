package com.simbiri.presentation.routes

import com.simbiri.application.community.CreateCommunitiesInBulkUseCase
import com.simbiri.application.community.CreateCommunityUseCase
import com.simbiri.application.community.DeleteCommunityUseCase
import com.simbiri.application.community.GetCommunitiesUseCase
import com.simbiri.application.community.GetCommunityByIdUseCase
import com.simbiri.application.community.UpdateCommunityUseCase
import com.simbiri.domain.model.community.Community
import com.simbiri.domain.util.ResultType
import com.simbiri.domain.util.onFailure
import com.simbiri.domain.util.onSuccess
import com.simbiri.presentation.routes.dto.community.CommunityUpsertDto
import com.simbiri.presentation.routes.dto.community.toCommResponseDto
import com.simbiri.presentation.routes.path.CommunityRoutesPath
import com.simbiri.presentation.utils.parseCommunityIdOrFailure
import com.simbiri.presentation.utils.parseOptionalCommunityOwnerIdOrFailure
import com.simbiri.presentation.utils.respondWithDataError
import com.simbiri.presentation.utils.toCommunityForCreateOrFailure
import com.simbiri.presentation.utils.toCommunityForUpdateOrFailure
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing

fun Routing.communityRoutes(
    createCommunityUseCase: CreateCommunityUseCase,
    createCommunitiesInBulkUseCase: CreateCommunitiesInBulkUseCase,
    getCommunityByIdUseCase: GetCommunityByIdUseCase,
    getCommunitiesUseCase: GetCommunitiesUseCase,
    updateCommunityUseCase: UpdateCommunityUseCase,
    deleteCommunityUseCase: DeleteCommunityUseCase,
) {

    // GET /communities?approved={value}&ownerId={uuid}
    get<CommunityRoutesPath> { path ->
        val ownerId = when (
            val parsed = parseOptionalCommunityOwnerIdOrFailure(
                operation = "getCommunities",
                rawOwnerId = path.ownerId,
            )
        ) {
            is ResultType.Success ->
                parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@get
            }
        }

        getCommunitiesUseCase(
            approved = path.approved,
            ownerId = ownerId,
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
        val communityId = when (
            val parsed = parseCommunityIdOrFailure(
                operation = "getCommunityById",
                rawCommunityId = path.communityId,
            )
        ) {
            is ResultType.Success ->
                parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@get
            }
        }

        getCommunityByIdUseCase(communityId)
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

    // POST /communities
    post<CommunityRoutesPath> {
        val dto = call.receive<CommunityUpsertDto>()

        val community = when (
            val parsed = dto.toCommunityForCreateOrFailure(
                operation = "createCommunity",
            )
        ) {
            is ResultType.Success ->
                parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@post
            }
        }

        createCommunityUseCase(community)
            .onSuccess {
                call.respond(
                    status = HttpStatusCode.Created,
                    message = "Community '${community.name}' " +
                            "created successfully.",
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // POST /communities/bulk
    post<CommunityRoutesPath.Bulk> {
        val requestDtos =
            call.receive<List<CommunityUpsertDto>>()

        val communities =
            mutableListOf<Community>()

        requestDtos.forEachIndexed { index, dto ->
            when (
                val parsed = dto.toCommunityForCreateOrFailure(
                    operation = "createCommunities.communities[$index]",
                )
            ) {
                is ResultType.Success ->
                    communities += parsed.data

                is ResultType.Failure -> {
                    respondWithDataError(parsed.error)
                    return@post
                }
            }
        }

        createCommunitiesInBulkUseCase(communities)
            .onSuccess {
                call.respond(
                    status = HttpStatusCode.Created,
                    message = "${communities.size} communities " +
                            "created successfully.",
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // PUT /communities/{communityId}
    put<CommunityRoutesPath.ById> { path ->
        val communityId = when (
            val parsed = parseCommunityIdOrFailure(
                operation = "updateCommunity",
                rawCommunityId = path.communityId,
            )
        ) {
            is ResultType.Success ->
                parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@put
            }
        }

        val dto = call.receive<CommunityUpsertDto>()

        val community = when (
            val parsed = dto.toCommunityForUpdateOrFailure(
                operation = "updateCommunity",
                communityId = communityId,
            )
        ) {
            is ResultType.Success ->
                parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@put
            }
        }

        updateCommunityUseCase(community)
            .onSuccess {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = "Community '${community.name}' " +
                            "updated successfully.",
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // DELETE /communities/{communityId}
    delete<CommunityRoutesPath.ById> { path ->
        val communityId = when (
            val parsed = parseCommunityIdOrFailure(
                operation = "deleteCommunityById",
                rawCommunityId = path.communityId,
            )
        ) {
            is ResultType.Success ->
                parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@delete
            }
        }

        deleteCommunityUseCase(communityId)
            .onSuccess {
                call.respond(
                    HttpStatusCode.NoContent,
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }
}