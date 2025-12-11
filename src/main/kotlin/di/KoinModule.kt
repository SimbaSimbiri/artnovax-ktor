package com.simbiri.di


import com.simbiri.data.database.DatabaseFactory
import com.simbiri.data.repository.UserRepoImpl
import com.simbiri.domain.repository.UserRepository
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {
    // our single rds db instance
    single<Database> { DatabaseFactory.create() }
    // our user repositories
    singleOf(::UserRepoImpl).bind<UserRepository>()
}
