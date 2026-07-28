package com.simbiri.di


import com.simbiri.data.database.DatabaseFactory
import com.simbiri.data.repository.CommunityRepoImpl
import com.simbiri.data.repository.TherapyContentRepoImpl
import com.simbiri.data.repository.UserRepoImpl
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.repository.CommunityRepository
import com.simbiri.domain.repository.TherapyContentRepository
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import com.simbiri.data.repository.AuthenticationCredentialRepoImpl
import com.simbiri.data.security.Argon2PasswordHasher
import com.simbiri.domain.repository.AuthenticationCredentialRepository
import com.simbiri.domain.security.PasswordHasher
import com.simbiri.domain.security.AccessTokenIssuer
import com.simbiri.presentation.auth.JwtAccessTokenIssuer
import com.simbiri.presentation.auth.JwtSettings

/**
 * Contains only infrastructure level dependencies
 */
val dataModule = module {
    // our single rds db instance
    single<Database> { DatabaseFactory.create() }

    // our user repositories
    singleOf(::UserRepoImpl).bind<UserRepository>()

    // Authentication credential repository
    singleOf(::AuthenticationCredentialRepoImpl).bind<AuthenticationCredentialRepository>()
    // Password hashing infrastructure
    single<PasswordHasher> { Argon2PasswordHasher() }
    // JWT signing and verification settings
    single { JwtSettings.fromEnvironment() }
    // Access-token signing infrastructure
    singleOf(::JwtAccessTokenIssuer).bind<AccessTokenIssuer>()

    // community repo
    singleOf(::CommunityRepoImpl).bind<CommunityRepository>()

    // therapy repo
    singleOf(::TherapyContentRepoImpl).bind<TherapyContentRepository>()
}
