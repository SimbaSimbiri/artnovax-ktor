package com.simbiri.di


import com.simbiri.data.database.DatabaseFactory
import com.simbiri.data.repository.AccessTokenSessionCommandRepoImpl
import com.simbiri.data.repository.AccessTokenSessionRepoImpl
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
import com.simbiri.data.repository.RefreshSessionRepoImpl
import com.simbiri.data.security.Argon2PasswordHasher
import com.simbiri.domain.repository.AuthenticationCredentialRepository
import com.simbiri.domain.security.PasswordHasher
import com.simbiri.domain.security.AccessTokenIssuer
import com.simbiri.presentation.auth.JwtAccessTokenIssuer
import com.simbiri.presentation.auth.JwtSettings
import com.simbiri.data.repository.UserRegistrationRepoImpl
import com.simbiri.data.security.SecureOpaqueRefreshTokenIssuer
import com.simbiri.domain.repository.AccessTokenSessionCommandRepository
import com.simbiri.domain.repository.AccessTokenSessionRepository
import com.simbiri.domain.repository.RefreshSessionCleanupRepository
import com.simbiri.domain.repository.RefreshSessionRepository
import com.simbiri.domain.repository.UserRegistrationRepository
import com.simbiri.domain.security.RefreshTokenIssuer
import com.simbiri.domain.security.RefreshTokenSettings
import com.simbiri.domain.repository.AuthenticationCredentialMutationRepository
import com.simbiri.data.storage.S3TherapyAssetUploadGateway
import com.simbiri.domain.storage.TherapyAssetUploadGateway
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import com.simbiri.data.repository.TherapyAssetRepoImpl
import com.simbiri.data.storage.S3TherapyAssetObjectStorage
import com.simbiri.domain.repository.TherapyAssetRepository
import com.simbiri.domain.storage.TherapyAssetObjectStorage
import software.amazon.awssdk.services.s3.S3Client


/**
 * Contains only infrastructure level dependencies
 */
val dataModule = module {
    // our single rds db instance
    single<Database> { DatabaseFactory.create() }

    // our user repositories
    singleOf(::UserRepoImpl).bind<UserRepository>()

    // Authentication credential repository
    single { AuthenticationCredentialRepoImpl( db = get(), clock = get(),) }
    single<AuthenticationCredentialRepository> { get<AuthenticationCredentialRepoImpl>() }
    single<AuthenticationCredentialMutationRepository> { get<AuthenticationCredentialRepoImpl>() }

    // Password hashing infrastructure
    single<PasswordHasher> { Argon2PasswordHasher() }
    // JWT signing and verification settings
    single { JwtSettings.fromEnvironment() }
    // Access-token signing infrastructure
    singleOf(::JwtAccessTokenIssuer).bind<AccessTokenIssuer>()
    // Access-token session validation
    singleOf(::AccessTokenSessionRepoImpl).bind<AccessTokenSessionRepository>()
    // Access-token session invalidation
    single<AccessTokenSessionCommandRepository> { AccessTokenSessionCommandRepoImpl(db = get(), clock = get()) }
    // Transactional account registration
    single<UserRegistrationRepository> { UserRegistrationRepoImpl(db = get(), clock = get()) }

    // Opaque refresh-token settings
    single { RefreshTokenSettings.fromEnvironment()}
    // Secure refresh-token generation
    single<RefreshTokenIssuer> { SecureOpaqueRefreshTokenIssuer( settings = get(), clock = get(),) }
    // refresh session and it's cleanup
    single { RefreshSessionRepoImpl( db = get(), clock = get(),) }
    single<RefreshSessionRepository> { get<RefreshSessionRepoImpl>() }
    single<RefreshSessionCleanupRepository> { get<RefreshSessionRepoImpl>() }

    // community repo
    singleOf(::CommunityRepoImpl).bind<CommunityRepository>()

    // therapy repo
    singleOf(::TherapyContentRepoImpl).bind<TherapyContentRepository>()
    singleOf(::TherapyAssetRepoImpl).bind<TherapyAssetRepository>()
    // s3 asset uploads for modules
    single {
        val regionName = System.getenv("AWS_REGION") ?: "us-east-1"
        S3Presigner.builder()
            .region(Region.of(regionName))
            .build()
    }

    single<TherapyAssetUploadGateway> {
        S3TherapyAssetUploadGateway(
            bucketName = System.getenv("ARTNOVAX_THERAPY_ASSET_BUCKET").orEmpty(),
            presigner = get<S3Presigner>(),
            clock = get(),
        )
    }
    single {
        val regionName = System.getenv("AWS_REGION") ?: "us-east-1"

        S3Client.builder()
            .region(Region.of(regionName))
            .build()
    }

    single<TherapyAssetObjectStorage> {
        S3TherapyAssetObjectStorage(
            bucketName = System.getenv("ARTNOVAX_THERAPY_ASSET_BUCKET").orEmpty(),
            s3Client = get<S3Client>(),
            presigner = get<S3Presigner>(),
            clock = get(),
        )
    }
}
