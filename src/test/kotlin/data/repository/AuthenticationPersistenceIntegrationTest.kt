package com.simbiri.data.repository

import com.simbiri.data.database.entity.auth.AuthenticationCredentialTable
import com.simbiri.data.database.entity.auth.RefreshSessionTable
import com.simbiri.data.database.entity.user.UserTable
import com.simbiri.domain.model.auth.AuthenticationAttemptMutationResult
import com.simbiri.domain.model.auth.AuthenticationCredential
import com.simbiri.domain.model.auth.PasswordHashAlgorithm
import com.simbiri.domain.model.auth.RefreshSession
import com.simbiri.domain.model.auth.RefreshSessionRotationResult
import com.simbiri.domain.model.common.RefreshSessionId
import com.simbiri.domain.model.common.RefreshTokenFamilyId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.user.UserType
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * PostgreSQL-backed integration coverage for security-sensitive
 * authentication persistence.
 *
 * These tests verify the real Exposed statements, database constraints,
 * row-locking behavior, and optimistic concurrency checks.
 */
class AuthenticationPersistenceIntegrationTest {
    private lateinit var credentialRepository: AuthenticationCredentialRepoImpl
    private lateinit var refreshSessionRepository: RefreshSessionRepoImpl

    @Before
    fun resetPersistenceState() {
        credentialRepository = AuthenticationCredentialRepoImpl(
            db = database,
            clock = CLOCK,
        )

        refreshSessionRepository = RefreshSessionRepoImpl(
            db = database,
            clock = CLOCK,
        )

        resetDatabase()
    }

    @Test
    fun `failed login attempt changes only attempt state`() = runBlocking {
        val result = credentialRepository.recordFailedLoginAttempt(
                userId = USER_ID,
                expectedPasswordHash = INITIAL_PASSWORD_HASH,
                expectedSessionVersion = INITIAL_SESSION_VERSION,
                attemptedAt = NOW,
            )

        val success = assertIs<ResultType.Success<AuthenticationAttemptMutationResult>>(result)

        val applied = assertIs<AuthenticationAttemptMutationResult.Applied>(success.data)

        assertEquals(
            expected = INITIAL_SESSION_VERSION,
            actual = applied.sessionVersion,
        )

        val persistedCredential = loadCredential()

        assertEquals(
            expected = 1,
            actual = persistedCredential.failedLoginAttempts,
        )

        assertNull(
            persistedCredential.lockedUntil
        )

        /*
         * Failed-login persistence must not overwrite password state or
         * session revocation state.
         */
        assertEquals(
            expected = INITIAL_PASSWORD_HASH,
            actual = persistedCredential.passwordHash,
        )

        assertEquals(
            expected = INITIAL_PASSWORD_UPDATED_AT,
            actual = persistedCredential.passwordUpdatedAt,
        )

        assertEquals(
            expected = INITIAL_SESSION_VERSION,
            actual = persistedCredential.sessionVersion,
        )
    }

    @Test
    fun `password replacement increments version and stale attempt cannot overwrite it`() = runBlocking {
        resetDatabase(
            failedLoginAttempts = 3,
            lockedUntil = NOW.plusSeconds(
                5L * 60L
            ),
        )

        val replacementResult = credentialRepository.replacePasswordAndIncrementSessionVersion(
                userId = USER_ID,
                expectedPasswordHash = INITIAL_PASSWORD_HASH,
                expectedSessionVersion = INITIAL_SESSION_VERSION,
                passwordHash = REPLACEMENT_PASSWORD_HASH,
                passwordAlgorithm = PasswordHashAlgorithm.ARGON2ID,
                passwordUpdatedAt = NOW,
            )

        val replacementSuccess = assertIs<ResultType.Success<Long>>(replacementResult)

        assertEquals(
            expected = 2L,
            actual = replacementSuccess.data,
        )

        val replacedCredential = loadCredential()

        assertEquals(
            expected = REPLACEMENT_PASSWORD_HASH,
            actual = replacedCredential.passwordHash,
        )

        assertEquals(
            expected = NOW,
            actual = replacedCredential.passwordUpdatedAt,
        )

        assertEquals(
            expected = 0,
            actual = replacedCredential.failedLoginAttempts,
        )

        assertNull(
            replacedCredential.lockedUntil
        )

        assertEquals(
            expected = 2L,
            actual = replacedCredential.sessionVersion,
        )

        /*
         * Simulate a failed-password request that authenticated against
         * the old credential before the password was replaced.
         */
        val staleAttemptResult = credentialRepository.recordFailedLoginAttempt(
                userId = USER_ID,
                expectedPasswordHash = INITIAL_PASSWORD_HASH,
                expectedSessionVersion = INITIAL_SESSION_VERSION,
                attemptedAt = NOW.plusSeconds(1L),
            )

        val staleAttemptSuccess = assertIs<ResultType.Success<AuthenticationAttemptMutationResult>>(staleAttemptResult)

        assertEquals(
            expected = AuthenticationAttemptMutationResult.StaleCredential,
            actual = staleAttemptSuccess.data,
        )

        val credentialAfterStaleAttempt = loadCredential()

        /*
         * The stale request must not restore the old password, old
         * sessionVersion, or failed-attempt state.
         */
        assertEquals(
            expected = REPLACEMENT_PASSWORD_HASH,
            actual = credentialAfterStaleAttempt.passwordHash,
        )

        assertEquals(
            expected = 2L,
            actual = credentialAfterStaleAttempt.sessionVersion,
        )

        assertEquals(
            expected = 0,
            actual = credentialAfterStaleAttempt.failedLoginAttempts,
        )
    }

    @Test
    fun `refresh rotation consumes old token and replay revokes replacement family`() = runBlocking {
        val familyId = RefreshTokenFamilyId(
            UUID.randomUUID()
        )

        createRefreshSession(
            tokenHash = FIRST_TOKEN_HASH,
            familyId = familyId,
        )

        val rotationResult = refreshSessionRepository.rotateSession(
                presentedTokenHash = FIRST_TOKEN_HASH,
                replacementTokenHash = SECOND_TOKEN_HASH,
                replacementExpiresAt = REFRESH_TOKEN_EXPIRY,
            )

        val rotationSuccess = assertIs<ResultType.Success<RefreshSessionRotationResult>>(rotationResult)

        val rotated = assertIs<RefreshSessionRotationResult.Rotated>(rotationSuccess.data)

        assertEquals(
            expected = USER_ID,
            actual = rotated.userId,
        )

        assertEquals(
            expected = INITIAL_SESSION_VERSION,
            actual = rotated.sessionVersion,
        )

        val consumedSession = loadRefreshSession(
            FIRST_TOKEN_HASH
        )

        assertNotNull(
            consumedSession.revokedAt
        )

        val replacementSession = loadRefreshSession(
            SECOND_TOKEN_HASH
        )

        assertNull(
            replacementSession.revokedAt
        )

        /*
         * Presenting the consumed token again represents refresh-token
         * reuse. The entire family must be revoked.
         */
        val replayResult = refreshSessionRepository.rotateSession(
                presentedTokenHash = FIRST_TOKEN_HASH,
                replacementTokenHash = THIRD_TOKEN_HASH,
                replacementExpiresAt = REFRESH_TOKEN_EXPIRY,
            )

        val replaySuccess = assertIs<ResultType.Success<RefreshSessionRotationResult>>(replayResult)

        assertEquals(
            expected = RefreshSessionRotationResult.ReuseDetected,
            actual = replaySuccess.data,
        )

        val replacementAfterReplay = loadRefreshSession(
            SECOND_TOKEN_HASH
        )

        assertNotNull(
            replacementAfterReplay.revokedAt
        )

        val unissuedReplacement = refreshSessionRepository.getSessionByTokenHash(
                THIRD_TOKEN_HASH
            )

        val missingReplacement = assertIs<ResultType.Failure<DataError>>(unissuedReplacement)

        assertEquals(
            expected = DataError.NotFound,
            actual = missingReplacement.error,
        )
    }

    @Test
    fun `credential version change invalidates existing refresh session`() = runBlocking {
        createRefreshSession(
            tokenHash = FIRST_TOKEN_HASH,
        )

        val replacementResult = credentialRepository.replacePasswordAndIncrementSessionVersion(
                userId = USER_ID,
                expectedPasswordHash = INITIAL_PASSWORD_HASH,
                expectedSessionVersion = INITIAL_SESSION_VERSION,
                passwordHash = REPLACEMENT_PASSWORD_HASH,
                passwordAlgorithm = PasswordHashAlgorithm.ARGON2ID,
                passwordUpdatedAt = NOW,
            )

        assertIs<ResultType.Success<Long>>(replacementResult)

        val rotationResult = refreshSessionRepository.rotateSession(
                presentedTokenHash = FIRST_TOKEN_HASH,
                replacementTokenHash = SECOND_TOKEN_HASH,
                replacementExpiresAt = REFRESH_TOKEN_EXPIRY,
            )

        val rotationSuccess = assertIs<ResultType.Success<RefreshSessionRotationResult>>(rotationResult)

        assertEquals(
            expected = RefreshSessionRotationResult.Invalid,
            actual = rotationSuccess.data,
        )

        val staleSession = loadRefreshSession(
            FIRST_TOKEN_HASH
        )

        assertNotNull(
            staleSession.revokedAt
        )

        val unissuedReplacement = refreshSessionRepository.getSessionByTokenHash(
                SECOND_TOKEN_HASH
            )

        val missingReplacement = assertIs<ResultType.Failure<DataError>>(unissuedReplacement)

        assertEquals(
            expected = DataError.NotFound,
            actual = missingReplacement.error,
        )
    }

    @Test
    fun `current device logout revokes every active token in family`() : Unit= runBlocking {
        val familyId = RefreshTokenFamilyId(
            UUID.randomUUID()
        )

        createRefreshSession(
            tokenHash = FIRST_TOKEN_HASH,
            familyId = familyId,
        )

        createRefreshSession(
            tokenHash = SECOND_TOKEN_HASH,
            familyId = familyId,
        )

        val logoutResult = refreshSessionRepository.revokeFamilyByTokenHash(
                SECOND_TOKEN_HASH
            )

        assertIs<ResultType.Success<Unit>>(logoutResult)

        assertNotNull(
            loadRefreshSession(
                FIRST_TOKEN_HASH
            ).revokedAt
        )

        assertNotNull(
            loadRefreshSession(
                SECOND_TOKEN_HASH
            ).revokedAt
        )

        /*
         * Unknown tokens remain successful no-ops so logout does not
         * disclose token validity.
         */
        val repeatedLogout = refreshSessionRepository.revokeFamilyByTokenHash(
                UNKNOWN_TOKEN_HASH
            )

        assertIs<ResultType.Success<Unit>>(repeatedLogout)
    }

    @Test
    fun `cleanup deletes only sessions older than retention cutoff`() : Unit = runBlocking {
        val cutoff = NOW.minusSeconds(
            7L * SECONDS_PER_DAY
        )

        insertRefreshSessionFixture(
            tokenHash = OLD_EXPIRED_TOKEN_HASH,
            expiresAt = NOW.minusSeconds(
                8L * SECONDS_PER_DAY
            ),
        )

        insertRefreshSessionFixture(
            tokenHash = RETAINED_EXPIRED_TOKEN_HASH,
            expiresAt = NOW.minusSeconds(
                6L * SECONDS_PER_DAY
            ),
        )

        val cleanupResult = refreshSessionRepository.deleteSessionsExpiredBefore(
                expiredBefore = cutoff,
                limit = 500,
            )

        val cleanupSuccess = assertIs<ResultType.Success<Int>>(cleanupResult)

        assertEquals(
            expected = 1,
            actual = cleanupSuccess.data,
        )

        val deletedSession = refreshSessionRepository.getSessionByTokenHash(
                OLD_EXPIRED_TOKEN_HASH
            )

        val deletedFailure = assertIs<ResultType.Failure<DataError>>(deletedSession)

        assertEquals(
            expected = DataError.NotFound,
            actual = deletedFailure.error,
        )

        val retainedSession = refreshSessionRepository.getSessionByTokenHash(
                RETAINED_EXPIRED_TOKEN_HASH
            )

        assertIs<ResultType.Success<RefreshSession>>(retainedSession)
    }

    /**
     * Recreates the minimum production schema required by authentication
     * persistence.
     */
    private fun resetDatabase(
        failedLoginAttempts: Int = 0,
        lockedUntil: Instant? = null,
    ) {
        transaction(database) {
            SchemaUtils.drop(
                RefreshSessionTable,
                AuthenticationCredentialTable,
                UserTable,
            )

            SchemaUtils.create(
                UserTable,
                AuthenticationCredentialTable,
                RefreshSessionTable,
            )

            UserTable.insert { row ->
                row[UserTable.id] = USER_ID.value
                row[UserTable.accountName] = "integration-user"
                row[UserTable.emailAddress] = "integration-user@example.com"
                row[UserTable.firstName] = "Integration"
                row[UserTable.lastName] = "User"
                row[UserTable.about] = null
                row[UserTable.tagline] = null
                row[UserTable.profileUrl] = null
                row[UserTable.backgroundUrl] = null
                row[UserTable.birthDate] = LocalDate.parse(
                    "2000-01-01"
                )

                row[UserTable.userType] = UserType.REGULAR.code
                row[UserTable.emailOptIn] = false
                row[UserTable.isPrivate] = true
                row[UserTable.isAnonymous] = false
                row[UserTable.isActive] = true
                row[UserTable.createdAt] = INITIAL_CREATED_AT
                row[UserTable.updatedAt] = INITIAL_CREATED_AT
            }

            AuthenticationCredentialTable.insert { row ->
                row[AuthenticationCredentialTable.userId] = USER_ID.value
                row[AuthenticationCredentialTable.passwordHash] = INITIAL_PASSWORD_HASH
                row[AuthenticationCredentialTable.passwordAlgorithm] = PasswordHashAlgorithm.ARGON2ID.name
                row[AuthenticationCredentialTable.passwordUpdatedAt] = INITIAL_PASSWORD_UPDATED_AT
                row[AuthenticationCredentialTable.failedLoginAttempts] = failedLoginAttempts
                row[AuthenticationCredentialTable.lockedUntil] = lockedUntil
                row[AuthenticationCredentialTable.sessionVersion] = INITIAL_SESSION_VERSION
                row[AuthenticationCredentialTable.createdAt] = INITIAL_CREATED_AT
                row[AuthenticationCredentialTable.updatedAt] = INITIAL_CREATED_AT
            }
        }
    }

    private suspend fun loadCredential(): AuthenticationCredential {
        val result = credentialRepository.getCredentialByUserId(
                USER_ID
            )

        return assertIs<ResultType.Success<AuthenticationCredential>>(result).data
    }

    private suspend fun createRefreshSession(
        tokenHash: String,
        familyId: RefreshTokenFamilyId = RefreshTokenFamilyId(
            UUID.randomUUID()
        ),
    ) {
        val result = refreshSessionRepository.createSession(
                RefreshSession(
                    familyId = familyId,
                    userId = USER_ID,
                    tokenHash = tokenHash,
                    sessionVersion = INITIAL_SESSION_VERSION,
                    expiresAt = REFRESH_TOKEN_EXPIRY,
                )
            )

        assertIs<ResultType.Success<RefreshSessionId>>(result)
    }

    private suspend fun loadRefreshSession(
        tokenHash: String,
    ): RefreshSession {
        val result = refreshSessionRepository.getSessionByTokenHash(
                tokenHash
            )

        return assertIs<ResultType.Success<RefreshSession>>(result).data
    }

    /**
     * Inserts historical refresh-session data for retention testing.
     *
     * The production creation repository always uses the current clock,
     * whereas this fixture needs internally consistent historical timestamps.
     */
    private fun insertRefreshSessionFixture(
        tokenHash: String,
        expiresAt: Instant,
    ) {
        val createdAt = expiresAt.minusSeconds(
            SECONDS_PER_DAY
        )

        transaction(database) {
            RefreshSessionTable.insert { row ->
                row[RefreshSessionTable.id] = UUID.randomUUID()
                row[RefreshSessionTable.userId] = USER_ID.value
                row[RefreshSessionTable.familyId] = UUID.randomUUID()
                row[RefreshSessionTable.tokenHash] = tokenHash
                row[RefreshSessionTable.sessionVersion] = INITIAL_SESSION_VERSION
                row[RefreshSessionTable.expiresAt] = expiresAt
                row[RefreshSessionTable.revokedAt] = expiresAt
                row[RefreshSessionTable.createdAt] = createdAt
                row[RefreshSessionTable.updatedAt] = expiresAt
            }
        }
    }

    companion object {

        private val postgres = PostgreSQLContainer(
            "postgres:16-alpine"
        ).withDatabaseName(
                "artnovax_auth_test"
            ).withUsername(
                "artnovax_test"
            ).withPassword(
                "artnovax_test"
            )
        private lateinit var database: Database
        private val NOW = Instant.parse(
            "2026-08-06T00:00:00Z"
        )
        private val CLOCK = Clock.fixed(
            NOW,
            ZoneOffset.UTC,
        )
        private val USER_ID = UserId(
            UUID.fromString(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
            )
        )
        private val INITIAL_CREATED_AT = Instant.parse(
            "2026-07-01T12:00:00Z"
        )
        private val INITIAL_PASSWORD_UPDATED_AT = Instant.parse(
            "2026-07-01T12:00:00Z"
        )
        private const val INITIAL_PASSWORD_HASH = "hash:initial-password"
        private const val REPLACEMENT_PASSWORD_HASH = "hash:replacement-password"
        private const val INITIAL_SESSION_VERSION = 1L
        private const val SECONDS_PER_DAY = 24L * 60L * 60L
        private val REFRESH_TOKEN_EXPIRY = NOW.plusSeconds(30L * SECONDS_PER_DAY)
        private val FIRST_TOKEN_HASH = "a".repeat(64)

        private val SECOND_TOKEN_HASH = "b".repeat(64)
        private val THIRD_TOKEN_HASH = "c".repeat(64)
        private val UNKNOWN_TOKEN_HASH = "d".repeat(64)
        private val OLD_EXPIRED_TOKEN_HASH = "e".repeat(64)
        private val RETAINED_EXPIRED_TOKEN_HASH = "f".repeat(64)

        @JvmStatic
        @BeforeClass
        fun startPostgreSql() {
            postgres.start()

            database = Database.connect(
                url = postgres.jdbcUrl,
                driver = postgres.driverClassName,
                user = postgres.username,
                password = postgres.password,
            )

            /*
             * Match the transaction isolation configured by DatabaseFactory.
             */
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ
        }

        @JvmStatic
        @AfterClass
        fun stopPostgreSql() {
            postgres.stop()
        }
    }
}
