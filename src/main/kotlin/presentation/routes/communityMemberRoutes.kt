package com.simbiri.presentation.routes

import com.simbiri.application.community.member.AddCommunityMemberUseCase
import com.simbiri.application.community.member.AddCommunityMembersInBulkUseCase
import com.simbiri.application.community.member.GetCommunityMembersUseCase
import com.simbiri.application.community.member.RemoveCommunityMemberUseCase
import com.simbiri.application.community.member.UpdateCommunityMemberRoleUseCase
import com.simbiri.domain.model.community.CommunityMemberAssignment
import com.simbiri.domain.util.ResultType
import com.simbiri.domain.util.onFailure
import com.simbiri.domain.util.onSuccess
import com.simbiri.presentation.routes.dto.community.CommunityMemberRoleUpdateDto
import com.simbiri.presentation.routes.dto.community.CommunityMemberUpsertDto
import com.simbiri.presentation.routes.dto.community.toMembersResponseDto
import com.simbiri.presentation.routes.path.CommunityRoutesPath
import com.simbiri.presentation.utils.parseCommunityIdOrFailure
import com.simbiri.presentation.utils.parseUserIdOrFailure
import com.simbiri.presentation.utils.respondWithDataError
import com.simbiri.presentation.utils.toMemberAssignmentOrFailure
import com.simbiri.presentation.utils.toMemberRoleOrFailure
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing

fun Routing.communityMemberRoutes(
    addCommunityMemberUseCase: AddCommunityMemberUseCase,
    addCommunityMembersInBulkUseCase:
    AddCommunityMembersInBulkUseCase,
    getCommunityMembersUseCase: GetCommunityMembersUseCase,
    updateCommunityMemberRoleUseCase:
    UpdateCommunityMemberRoleUseCase,
    removeCommunityMemberUseCase: RemoveCommunityMemberUseCase,
) {

    // GET /communities/{communityId}/members
    get<CommunityRoutesPath.Members> { path ->
        val communityId = when (
            val parsed = parseCommunityIdOrFailure(
                operation = "getCommunityMembers",
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

        getCommunityMembersUseCase(communityId)
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

    // POST /communities/{communityId}/members
    post<CommunityRoutesPath.Members> { path ->
        val communityId = when (
            val parsed = parseCommunityIdOrFailure(
                operation = "addCommunityMember",
                rawCommunityId = path.communityId,
            )
        ) {
            is ResultType.Success ->
                parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@post
            }
        }

        val dto = call.receive<CommunityMemberUpsertDto>()

        val assignment = when (
            val parsed = dto.toMemberAssignmentOrFailure(
                operation = "addCommunityMember",
            )
        ) {
            is ResultType.Success ->
                parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@post
            }
        }

        addCommunityMemberUseCase(
            communityId = communityId,
            userId = assignment.userId,
            role = assignment.role,
        )
            .onSuccess {
                call.respond(
                    status = HttpStatusCode.Created,
                    message = "User '${assignment.userId.value}' added " +
                            "to community '${communityId.value}' " +
                            "with role '${assignment.role}'.",
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // POST /communities/{communityId}/members/bulk
    post<CommunityRoutesPath.Members.Bulk> { path ->
        val communityId = when (
            val parsed = parseCommunityIdOrFailure(
                operation = "addCommunityMembers",
                rawCommunityId = path.parent.communityId,
            )
        ) {
            is ResultType.Success ->
                parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@post
            }
        }

        val requestDtos =
            call.receive<List<CommunityMemberUpsertDto>>()

        val assignments =
            mutableListOf<CommunityMemberAssignment>()

        requestDtos.forEachIndexed { index, dto ->
            when (
                val parsed = dto.toMemberAssignmentOrFailure(
                    operation = "addCommunityMembers.members[$index]",
                )
            ) {
                is ResultType.Success ->
                    assignments += parsed.data

                is ResultType.Failure -> {
                    respondWithDataError(parsed.error)
                    return@post
                }
            }
        }

        addCommunityMembersInBulkUseCase(
            communityId = communityId,
            assignments = assignments,
        )
            .onSuccess {
                call.respond(
                    status = HttpStatusCode.Created,
                    message = "${assignments.size} members added " +
                            "successfully to community " +
                            "'${communityId.value}'.",
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // PUT /communities/{communityId}/members/{userId}
    put<CommunityRoutesPath.Members.MemberById> { path ->
        val communityId = when (
            val parsed = parseCommunityIdOrFailure(
                operation = "updateCommunityMemberRole",
                rawCommunityId = path.parent.communityId,
            )
        ) {
            is ResultType.Success ->
                parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@put
            }
        }

        val userId = when (
            val parsed = parseUserIdOrFailure(
                operation = "updateCommunityMemberRole",
                rawUserId = path.userId,
            )
        ) {
            is ResultType.Success ->
                parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@put
            }
        }

        val dto =
            call.receive<CommunityMemberRoleUpdateDto>()

        val role = when (
            val parsed = dto.toMemberRoleOrFailure(
                operation = "updateCommunityMemberRole",
            )
        ) {
            is ResultType.Success ->
                parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@put
            }
        }

        updateCommunityMemberRoleUseCase(
            communityId = communityId,
            userId = userId,
            role = role,
        )
            .onSuccess {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = "Role for user '${userId.value}' in " +
                            "community '${communityId.value}' updated " +
                            "to '$role'.",
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // DELETE /communities/{communityId}/members/{userId}
    delete<CommunityRoutesPath.Members.MemberById> { path ->
        val communityId = when (
            val parsed = parseCommunityIdOrFailure(
                operation = "removeCommunityMember",
                rawCommunityId = path.parent.communityId,
            )
        ) {
            is ResultType.Success ->
                parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@delete
            }
        }

        val userId = when (
            val parsed = parseUserIdOrFailure(
                operation = "removeCommunityMember",
                rawUserId = path.userId,
            )
        ) {
            is ResultType.Success ->
                parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@delete
            }
        }

        removeCommunityMemberUseCase(
            communityId = communityId,
            userId = userId,
        )
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