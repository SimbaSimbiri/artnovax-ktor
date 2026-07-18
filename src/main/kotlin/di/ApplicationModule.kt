package com.simbiri.di

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
 * Provides application layer dependencies
 *
 */

val applicationModule = module {

    /**
     * we use a single app clock for date-dependent behaviour
     */
    single<Clock> { Clock.systemUTC() }

    // User use cases
    singleOf(::CreateUserUseCase)
    singleOf(::CreateUsersInBulkUseCase)
    singleOf(::GetUserByIdUseCase)
    singleOf(::GetUsersUseCase)
    singleOf(::UpdateUserUseCase)
    singleOf(::DeleteUserUseCase)
}