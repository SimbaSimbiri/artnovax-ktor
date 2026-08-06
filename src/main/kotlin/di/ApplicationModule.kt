package com.simbiri.di

import com.simbiri.application.auth.AuthenticateUserUseCase
import com.simbiri.application.auth.ChangeCurrentUserPasswordUseCase
import com.simbiri.application.auth.CleanupRefreshSessionsUseCase
import com.simbiri.application.auth.LogoutAllDevicesUseCase
import com.simbiri.application.auth.LogoutCurrentDeviceUseCase
import com.simbiri.application.auth.ProvisionAuthenticationCredentialUseCase
import com.simbiri.application.auth.RefreshAccessTokenUseCase
import com.simbiri.application.auth.RegisterUserUseCase
import com.simbiri.application.auth.ValidateAccessTokenSessionUseCase
import com.simbiri.application.community.*
import com.simbiri.application.community.member.*
import com.simbiri.application.therapy.context.TherapyContentContextLoader
import com.simbiri.application.therapy.lifecycle.ArchiveTherapyContentUseCase
import com.simbiri.application.therapy.lifecycle.PublishTherapyContentUseCase
import com.simbiri.application.therapy.lifecycle.ReturnTherapyContentToDraftUseCase
import com.simbiri.application.therapy.lifecycle.SubmitTherapyContentForReviewUseCase
import com.simbiri.application.therapy.module.AddTherapyModuleUseCase
import com.simbiri.application.therapy.module.RemoveTherapyModuleUseCase
import com.simbiri.application.therapy.module.ReorderTherapyModulesUseCase
import com.simbiri.application.therapy.module.UpdateTherapyModuleUseCase
import com.simbiri.application.therapy.query.*
import com.simbiri.application.therapy.session.CreateTherapyDraftUseCase
import com.simbiri.application.therapy.session.DeleteTherapyDraftUseCase
import com.simbiri.application.therapy.session.UpdateTherapyDraftUseCase
import com.simbiri.application.user.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.time.Clock
import com.simbiri.application.therapy.asset.RequestTherapyAssetUploadUseCase
import com.simbiri.application.therapy.asset.TherapyAssetStorageKeyFactory
/**
 * Provides application-layer dependencies.
 *
 * Repository and database definitions remain in dataModule.
 */
val applicationModule = module {

    /*
     * One application clock keeps date-dependent behavior consistent
     * and allows deterministic replacement in tests.
     */
    single<Clock> {
        Clock.systemUTC()
    }

    // Authentication credential operations
    singleOf(::ValidateAccessTokenSessionUseCase)
    singleOf(::ProvisionAuthenticationCredentialUseCase)
    singleOf(::AuthenticateUserUseCase)
    singleOf(::RegisterUserUseCase)
    singleOf(::ChangeCurrentUserPasswordUseCase)
    singleOf(::LogoutAllDevicesUseCase)
    singleOf(::LogoutCurrentDeviceUseCase)
    singleOf(::RefreshAccessTokenUseCase)
    singleOf(::CleanupRefreshSessionsUseCase )
    // User use cases
    singleOf(::CreateUserUseCase)
    singleOf(::CreateUsersInBulkUseCase)
    singleOf(::GetUserByIdUseCase)
    singleOf(::GetUsersUseCase)
    singleOf(::UpdateUserUseCase)
    singleOf(::DeleteUserUseCase)
    // Public and authenticated profile ops
    singleOf(::GetPublicUsersUseCase)
    singleOf(::GetPublicUserByIdUseCase)
    singleOf(::GetCurrentUserUseCase)
    singleOf(::UpdateCurrentUserProfileUseCase)

    // Community use cases
    singleOf(::CreateCommunityUseCase)
    singleOf(::CreateCommunitiesInBulkUseCase)
    singleOf(::GetCommunityByIdUseCase)
    singleOf(::GetCommunitiesUseCase)
    singleOf(::UpdateCommunityUseCase)
    singleOf(::DeleteCommunityUseCase)

    // Community membership use cases
    singleOf(::AddCommunityMemberUseCase)
    singleOf(::AddCommunityMembersInBulkUseCase)
    singleOf(::GetCommunityMembersUseCase)
    singleOf(::UpdateCommunityMemberRoleUseCase)
    singleOf(::RemoveCommunityMemberUseCase)

    // Therapy-content application support
    singleOf(::TherapyContentContextLoader)

    // Therapy draft operations
    singleOf(::CreateTherapyDraftUseCase)
    singleOf(::UpdateTherapyDraftUseCase)
    singleOf(::DeleteTherapyDraftUseCase)

    // Therapy module operations
    singleOf(::AddTherapyModuleUseCase)
    singleOf(::UpdateTherapyModuleUseCase)
    singleOf(::ReorderTherapyModulesUseCase)
    singleOf(::RemoveTherapyModuleUseCase)

    // Therapy lifecycle operations
    singleOf(::SubmitTherapyContentForReviewUseCase)
    singleOf(::ReturnTherapyContentToDraftUseCase)
    singleOf(::PublishTherapyContentUseCase)
    singleOf(::ArchiveTherapyContentUseCase)

    // Therapy-content queries
    singleOf(::GetPublishedTherapySessionsUseCase)
    singleOf(::GetPublishedTherapySessionByIdUseCase)
    singleOf(::GetManagedTherapySessionsUseCase)
    singleOf(::GetManagedTherapySessionByIdUseCase)
    singleOf(::GetLatestManagedTherapySessionVersionUseCase)

    // s3 asset uploads
    // Therapy asset upload workflow
    single { TherapyAssetStorageKeyFactory() }
    singleOf(::RequestTherapyAssetUploadUseCase)
}
