package com.simbiri.presentation.config

import com.simbiri.application.auth.AuthenticateUserUseCase
import com.simbiri.application.auth.ChangeCurrentUserPasswordUseCase
import com.simbiri.application.auth.RegisterUserUseCase
import com.simbiri.application.community.*
import com.simbiri.application.community.member.*
import com.simbiri.application.therapy.query.*
import com.simbiri.application.user.*
import com.simbiri.presentation.routes.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    install(Resources)

    // Public user-profile ops
    val getPublicUserByIdUseCase: GetPublicUserByIdUseCase by inject()
    val getPublicUsersUseCase: GetPublicUsersUseCase by inject()

    // Authenticated current-user profile
    val getCurrentUserUseCase: GetCurrentUserUseCase by inject()
    val updateCurrentUserProfileUseCase: UpdateCurrentUserProfileUseCase by inject()

    // Password authentication
    val authenticateUserUseCase: AuthenticateUserUseCase by inject()
    val registerUserUseCase: RegisterUserUseCase by inject()
    val changeCurrentUserPasswordUseCase: ChangeCurrentUserPasswordUseCase by inject()

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

    // Public therapy-content query use cases
    val getPublishedTherapySessionsUseCase: GetPublishedTherapySessionsUseCase by inject()
    val getPublishedTherapySessionByIdUseCase: GetPublishedTherapySessionByIdUseCase by inject()
    // Managed therapy-content query use cases
    val getManagedTherapySessionsUseCase: GetManagedTherapySessionsUseCase by inject()
    val getManagedTherapySessionByIdUseCase: GetManagedTherapySessionByIdUseCase by inject()
    val getLatestManagedTherapySessionVersionUseCase: GetLatestManagedTherapySessionVersionUseCase by inject()

    routing {
        root()

        authenticationRoutes(authenticateUserUseCase = authenticateUserUseCase)
        registrationRoutes(registerUserUseCase = registerUserUseCase)

        userRoutes(
            getPublicUserByIdUseCase = getPublicUserByIdUseCase,
            getPublicUsersUseCase = getPublicUsersUseCase,
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

        publishedTherapyRoutes(
            getPublishedTherapySessionsUseCase = getPublishedTherapySessionsUseCase,
            getPublishedTherapySessionByIdUseCase = getPublishedTherapySessionByIdUseCase,
        )

        authenticate(JWT_AUTH_PROVIDER) {
            currentUserRoutes(
                getCurrentUserUseCase = getCurrentUserUseCase,
                updateCurrentUserProfileUseCase = updateCurrentUserProfileUseCase
            )

            currentUserPasswordRoutes(
                changeCurrentUserPasswordUseCase = changeCurrentUserPasswordUseCase,
            )

            managedTherapyRoutes(
                getManagedTherapySessionsUseCase = getManagedTherapySessionsUseCase,
                getManagedTherapySessionByIdUseCase = getManagedTherapySessionByIdUseCase,
                getLatestManagedTherapySessionVersionUseCase = getLatestManagedTherapySessionVersionUseCase,
            )
        }
    }
}
