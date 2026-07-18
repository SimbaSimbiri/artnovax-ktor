package com.simbiri.presentation.config

import com.simbiri.application.community.CreateCommunitiesInBulkUseCase
import com.simbiri.application.community.CreateCommunityUseCase
import com.simbiri.application.community.DeleteCommunityUseCase
import com.simbiri.application.community.GetCommunitiesUseCase
import com.simbiri.application.community.GetCommunityByIdUseCase
import com.simbiri.application.community.UpdateCommunityUseCase
import com.simbiri.application.community.member.AddCommunityMemberUseCase
import com.simbiri.application.community.member.AddCommunityMembersInBulkUseCase
import com.simbiri.application.community.member.GetCommunityMembersUseCase
import com.simbiri.application.community.member.RemoveCommunityMemberUseCase
import com.simbiri.application.community.member.UpdateCommunityMemberRoleUseCase
import com.simbiri.application.user.CreateUserUseCase
import com.simbiri.application.user.CreateUsersInBulkUseCase
import com.simbiri.application.user.DeleteUserUseCase
import com.simbiri.application.user.GetUserByIdUseCase
import com.simbiri.application.user.GetUsersUseCase
import com.simbiri.application.user.UpdateUserUseCase
import com.simbiri.domain.repository.CommunityRepository
import com.simbiri.presentation.routes.communityMemberRoutes
import com.simbiri.presentation.routes.communityRoutes
import com.simbiri.presentation.routes.root
import com.simbiri.presentation.routes.userRoutes
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.resources.Resources
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Application.configureRouting() {
    install(Resources)

    // User application use cases
    val createUserUseCase: CreateUserUseCase by inject()
    val createUsersInBulkUseCase: CreateUsersInBulkUseCase by inject()
    val getUserByIdUseCase: GetUserByIdUseCase by inject()
    val getUsersUseCase: GetUsersUseCase by inject()
    val updateUserUseCase: UpdateUserUseCase by inject()
    val deleteUserUseCase: DeleteUserUseCase by inject()

    // Community use cases
    val createCommunityUseCase: CreateCommunityUseCase by inject()
    val createCommunitiesInBulkUseCase: CreateCommunitiesInBulkUseCase by inject()
    val getCommunityByIdUseCase: GetCommunityByIdUseCase by inject()
    val getCommunitiesUseCase: GetCommunitiesUseCase by inject()
    val updateCommunityUseCase: UpdateCommunityUseCase by inject()
    val deleteCommunityUseCase: DeleteCommunityUseCase by inject()

    // Community membership use cases
    val addCommunityMemberUseCase: AddCommunityMemberUseCase by inject()
    val addCommunityMembersInBulkUseCase: AddCommunityMembersInBulkUseCase by inject()
    val getCommunityMembersUseCase: GetCommunityMembersUseCase by inject()
    val updateCommunityMemberRoleUseCase: UpdateCommunityMemberRoleUseCase by inject()
    val removeCommunityMemberUseCase: RemoveCommunityMemberUseCase by inject()

    routing {
        root()

        userRoutes(
            createUserUseCase = createUserUseCase,
            createUsersInBulkUseCase = createUsersInBulkUseCase,
            getUserByIdUseCase = getUserByIdUseCase,
            getUsersUseCase = getUsersUseCase,
            updateUserUseCase = updateUserUseCase,
            deleteUserUseCase = deleteUserUseCase,
        )

        communityRoutes(
            createCommunityUseCase = createCommunityUseCase,
            createCommunitiesInBulkUseCase = createCommunitiesInBulkUseCase,
            getCommunityByIdUseCase = getCommunityByIdUseCase,
            getCommunitiesUseCase = getCommunitiesUseCase,
            updateCommunityUseCase = updateCommunityUseCase,
            deleteCommunityUseCase = deleteCommunityUseCase,
        )

        communityMemberRoutes(
            addCommunityMemberUseCase = addCommunityMemberUseCase,
            addCommunityMembersInBulkUseCase = addCommunityMembersInBulkUseCase,
            getCommunityMembersUseCase = getCommunityMembersUseCase,
            updateCommunityMemberRoleUseCase = updateCommunityMemberRoleUseCase,
            removeCommunityMemberUseCase = removeCommunityMemberUseCase,
        )
    }
}