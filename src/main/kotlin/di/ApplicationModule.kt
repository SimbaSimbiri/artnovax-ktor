package com.simbiri.di

import com.simbiri.application.community.*
import com.simbiri.application.community.member.*
import com.simbiri.application.lifecycle.therapy.SubmitTherapyContentForReviewUseCase
import com.simbiri.application.therapy.AddTherapyModuleUseCase
import com.simbiri.application.therapy.context.TherapyContentContextLoader
import com.simbiri.application.therapy.lifecycle.ArchiveTherapyContentUseCase
import com.simbiri.application.therapy.lifecycle.PublishTherapyContentUseCase
import com.simbiri.application.therapy.lifecycle.ReturnTherapyContentToDraftUseCase
import com.simbiri.application.therapy.module.RemoveTherapyModuleUseCase
import com.simbiri.application.therapy.module.ReorderTherapyModulesUseCase
import com.simbiri.application.therapy.module.UpdateTherapyModuleUseCase
import com.simbiri.application.therapy.session.CreateTherapyDraftUseCase
import com.simbiri.application.therapy.session.DeleteTherapyDraftUseCase
import com.simbiri.application.therapy.session.UpdateTherapyDraftUseCase
import com.simbiri.application.user.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.time.Clock

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

    // User use cases
    singleOf(::CreateUserUseCase)
    singleOf(::CreateUsersInBulkUseCase)
    singleOf(::GetUserByIdUseCase)
    singleOf(::GetUsersUseCase)
    singleOf(::UpdateUserUseCase)
    singleOf(::DeleteUserUseCase)

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
}