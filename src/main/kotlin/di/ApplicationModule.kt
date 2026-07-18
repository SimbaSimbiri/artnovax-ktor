package com.simbiri.di

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
}