package com.simbiri.presentation.config

import com.simbiri.application.auth.AuthenticateUserUseCase
import com.simbiri.application.auth.ChangeCurrentUserPasswordUseCase
import com.simbiri.application.auth.LogoutAllDevicesUseCase
import com.simbiri.application.auth.LogoutCurrentDeviceUseCase
import com.simbiri.application.auth.RefreshAccessTokenUseCase
import com.simbiri.application.auth.RegisterUserUseCase
import com.simbiri.application.community.*
import com.simbiri.application.community.member.*
import com.simbiri.application.therapy.module.AddTherapyModuleUseCase
import com.simbiri.application.therapy.module.RemoveTherapyModuleUseCase
import com.simbiri.application.therapy.module.ReorderTherapyModulesUseCase
import com.simbiri.application.therapy.module.UpdateTherapyModuleUseCase
import com.simbiri.application.therapy.query.*
import com.simbiri.application.user.*
import com.simbiri.presentation.routes.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import kotlin.getValue
import com.simbiri.application.therapy.session.CreateTherapyDraftUseCase
import com.simbiri.application.therapy.session.DeleteTherapyDraftUseCase
import com.simbiri.application.therapy.session.UpdateTherapyDraftUseCase
import com.simbiri.application.therapy.lifecycle.ArchiveTherapyContentUseCase
import com.simbiri.application.therapy.lifecycle.PublishTherapyContentUseCase
import com.simbiri.application.therapy.lifecycle.ReturnTherapyContentToDraftUseCase
import com.simbiri.application.therapy.lifecycle.SubmitTherapyContentForReviewUseCase
import com.simbiri.application.therapy.asset.RequestTherapyAssetUploadUseCase
import com.simbiri.application.therapy.asset.ConfirmTherapyAssetUploadUseCase
import com.simbiri.application.therapy.asset.GetManagedTherapyAssetDownloadUseCase
import com.simbiri.application.therapy.asset.GetPublishedTherapyAssetDownloadUseCase


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
    val logoutAllDevicesUseCase: LogoutAllDevicesUseCase by inject()
    val logoutCurrentDeviceUseCase: LogoutCurrentDeviceUseCase by inject()
    val refreshAccessTokenUseCase: RefreshAccessTokenUseCase by inject()

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
    // Managed therapy-content draft mutations
    val createTherapyDraftUseCase: CreateTherapyDraftUseCase by inject()
    val updateTherapyDraftUseCase: UpdateTherapyDraftUseCase by inject()
    val deleteTherapyDraftUseCase: DeleteTherapyDraftUseCase by inject()
    // Managed therapy-module mutations
    val addTherapyModuleUseCase: AddTherapyModuleUseCase by inject()
    val updateTherapyModuleUseCase: UpdateTherapyModuleUseCase by inject()
    val reorderTherapyModulesUseCase: ReorderTherapyModulesUseCase by inject()
    val removeTherapyModuleUseCase: RemoveTherapyModuleUseCase by inject()
    // Managed therapy-content lifecycle mutations
    val submitTherapyContentForReviewUseCase: SubmitTherapyContentForReviewUseCase by inject()
    val returnTherapyContentToDraftUseCase: ReturnTherapyContentToDraftUseCase by inject()
    val publishTherapyContentUseCase: PublishTherapyContentUseCase by inject()
    val archiveTherapyContentUseCase: ArchiveTherapyContentUseCase by inject()
    // Managed therapy-asset uploads
    val requestTherapyAssetUploadUseCase: RequestTherapyAssetUploadUseCase by inject()
    // Therapy asset workflows
    val confirmTherapyAssetUploadUseCase: ConfirmTherapyAssetUploadUseCase by inject()
    val getPublishedTherapyAssetDownloadUseCase: GetPublishedTherapyAssetDownloadUseCase by inject()
    val getManagedTherapyAssetDownloadUseCase: GetManagedTherapyAssetDownloadUseCase by inject()


    routing {
        root()

        authenticationRoutes(authenticateUserUseCase = authenticateUserUseCase)
        refreshAuthenticationRoutes (refreshAccessTokenUseCase = refreshAccessTokenUseCase)
        logoutAuthenticationRoutes(logoutCurrentDeviceUseCase =logoutCurrentDeviceUseCase,)
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

        publishedTherapyAssetRoutes(
            getPublishedTherapyAssetDownloadUseCase = getPublishedTherapyAssetDownloadUseCase,
        )

        authenticate(JWT_AUTH_PROVIDER) {
            currentUserRoutes(
                getCurrentUserUseCase = getCurrentUserUseCase,
                updateCurrentUserProfileUseCase = updateCurrentUserProfileUseCase
            )

            currentUserPasswordRoutes(
                changeCurrentUserPasswordUseCase = changeCurrentUserPasswordUseCase,
            )

            currentUserSessionRoutes(
                logoutAllDevicesUseCase = logoutAllDevicesUseCase,
            )

            managedTherapyRoutes(
                getManagedTherapySessionsUseCase = getManagedTherapySessionsUseCase,
                getManagedTherapySessionByIdUseCase = getManagedTherapySessionByIdUseCase,
                getLatestManagedTherapySessionVersionUseCase = getLatestManagedTherapySessionVersionUseCase,
            )
            managedTherapyMutationRoutes(
                createTherapyDraftUseCase = createTherapyDraftUseCase,
                updateTherapyDraftUseCase = updateTherapyDraftUseCase,
                deleteTherapyDraftUseCase = deleteTherapyDraftUseCase,
            )
            managedTherapyModuleRoutes(
                addTherapyModuleUseCase = addTherapyModuleUseCase,
                updateTherapyModuleUseCase = updateTherapyModuleUseCase,
                reorderTherapyModulesUseCase = reorderTherapyModulesUseCase,
                removeTherapyModuleUseCase = removeTherapyModuleUseCase,
            )
            managedTherapyLifecycleRoutes(
                submitTherapyContentForReviewUseCase = submitTherapyContentForReviewUseCase,
                returnTherapyContentToDraftUseCase = returnTherapyContentToDraftUseCase,
                publishTherapyContentUseCase = publishTherapyContentUseCase,
                archiveTherapyContentUseCase = archiveTherapyContentUseCase,
            )
            managedTherapyAssetUploadRoutes(
                requestTherapyAssetUploadUseCase = requestTherapyAssetUploadUseCase,
                confirmTherapyAssetUploadUseCase = confirmTherapyAssetUploadUseCase,
            )
            managedTherapyAssetRoutes(
                getManagedTherapyAssetDownloadUseCase = getManagedTherapyAssetDownloadUseCase,
            )
        }
    }
}
