package com.simbiri.application.auth

import com.simbiri.domain.model.auth.AuthenticationAttemptMutationResult
import com.simbiri.domain.model.auth.AuthenticationCredential
import com.simbiri.domain.model.auth.PasswordChangeError
import com.simbiri.domain.model.auth.PasswordHashAlgorithm
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.user.User
import com.simbiri.domain.model.user.UserType
import com.simbiri.domain.repository.AuthenticationCredentialMutationRepository
import com.simbiri.domain.repository.AuthenticationCredentialRepository
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.security.PasswordHasher
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import kotlinx.coroutines.runBlocking
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChangeCurrentUserPasswordUseCaseTest {

    private val changedAt = Instant.parse(
        "2026-07-29T17:00:00Z"
    )

    private val clock = Clock.fixed(
        changedAt,
        ZoneOffset.UTC,
    )

    @Test
    fun `valid current password replaces credential and clears failures`() = runBlocking {
        val user = activeUser()

        val userId = requireNotNull(
            user.id
        )

        val storedCredential = credential(
            userId = userId,
            failedLoginAttempts = 3,
        )

        val credentialRepository = CredentialRepositoryFake(
            storedCredential = storedCredential
        )

        val mutationRepository = AuthenticationCredentialMutationRepositoryFake(
            passwordReplacementResult = ResultType.Success(2L)
        )

        val passwordHasher = PasswordHasherFake()

        val useCase = ChangeCurrentUserPasswordUseCase(
            userRepository = UserRepositoryFake(user),
            credentialRepository = credentialRepository,
            credentialMutationRepository = mutationRepository,
            passwordHasher = passwordHasher,
            clock = clock,
        )

        val currentPassword = "current-password-123".toCharArray()

        val newPassword = "replacement-password-123".toCharArray()

        try {
            val result = useCase(
                authenticatedUserId = userId,
                currentPassword = currentPassword,
                newPassword = newPassword,
            )

            assertIs<ResultType.Success<Unit>>(result)

            /*
             * This use-case test verifies the command sent to the
             * mutation repository. The mutation repository's actual
             * PostgreSQL update should be tested separately.
             */
            val replacementRequest = mutationRepository.passwordReplacementRequests.single()

            assertEquals(
                expected = userId,
                actual = replacementRequest.userId,
            )

            assertEquals(
                expected = "hash:current-password-123",
                actual = replacementRequest.expectedPasswordHash,
            )

            assertEquals(
                expected = 1L,
                actual = replacementRequest.expectedSessionVersion,
            )

            assertEquals(
                expected = "hash:replacement-password-123",
                actual = replacementRequest.passwordHash,
            )

            assertEquals(
                expected = PasswordHashAlgorithm.ARGON2ID,
                actual = replacementRequest.passwordAlgorithm,
            )

            assertEquals(
                expected = changedAt,
                actual = replacementRequest.passwordUpdatedAt,
            )

            assertEquals(
                expected = 1,
                actual = passwordHasher.hashCalls,
            )

            assertTrue(
                mutationRepository.failedAttemptRequests.isEmpty()
            )

            assertTrue(
                mutationRepository.successfulAttemptRequests.isEmpty()
            )
        } finally {
            currentPassword.fill(
                '\u0000'
            )

            newPassword.fill(
                '\u0000'
            )
        }
    }

    @Test
    fun `incorrect current password records failed attempt`() = runBlocking {
        val user = activeUser()

        val userId = requireNotNull(
            user.id
        )

        val storedCredential = credential(
            userId = userId
        )

        val credentialRepository = CredentialRepositoryFake(
            storedCredential = storedCredential
        )

        val mutationRepository = AuthenticationCredentialMutationRepositoryFake(
            failedAttemptResult = ResultType.Success(
                AuthenticationAttemptMutationResult.Applied(
                        sessionVersion = 1L
                    )
            )
        )

        val passwordHasher = PasswordHasherFake()

        val useCase = ChangeCurrentUserPasswordUseCase(
            userRepository = UserRepositoryFake(user),
            credentialRepository = credentialRepository,
            credentialMutationRepository = mutationRepository,
            passwordHasher = passwordHasher,
            clock = clock,
        )

        val currentPassword = "incorrect-password".toCharArray()

        val newPassword = "replacement-password-123".toCharArray()

        try {
            val result = useCase(
                authenticatedUserId = userId,
                currentPassword = currentPassword,
                newPassword = newPassword,
            )

            val failure = assertIs<ResultType.Failure<PasswordChangeError>>(result)

            assertEquals(
                expected = PasswordChangeError.InvalidCurrentPassword,
                actual = failure.error,
            )

            val failedAttemptRequest = mutationRepository.failedAttemptRequests.single()

            assertEquals(
                expected = userId,
                actual = failedAttemptRequest.userId,
            )

            assertEquals(
                expected = "hash:current-password-123",
                actual = failedAttemptRequest.expectedPasswordHash,
            )

            assertEquals(
                expected = 1L,
                actual = failedAttemptRequest.expectedSessionVersion,
            )

            assertEquals(
                expected = changedAt,
                actual = failedAttemptRequest.attemptedAt,
            )

            /*
             * An invalid current password must not hash or persist the
             * proposed replacement password.
             */
            assertEquals(
                expected = 0,
                actual = passwordHasher.hashCalls,
            )

            assertTrue(
                mutationRepository.passwordReplacementRequests.isEmpty()
            )

            assertTrue(
                mutationRepository.successfulAttemptRequests.isEmpty()
            )
        } finally {
            currentPassword.fill(
                '\u0000'
            )

            newPassword.fill(
                '\u0000'
            )
        }
    }

    @Test
    fun `new password must differ from current password`() = runBlocking {
        val user = activeUser()

        val userId = requireNotNull(
            user.id
        )

        val credentialRepository = CredentialRepositoryFake(
            storedCredential = credential(
                userId = userId
            )
        )

        val mutationRepository = AuthenticationCredentialMutationRepositoryFake()

        val passwordHasher = PasswordHasherFake()

        val useCase = ChangeCurrentUserPasswordUseCase(
            userRepository = UserRepositoryFake(user),
            credentialRepository = credentialRepository,
            credentialMutationRepository = mutationRepository,
            passwordHasher = passwordHasher,
            clock = clock,
        )

        val currentPassword = "current-password-123".toCharArray()

        val newPassword = currentPassword.copyOf()

        try {
            val result = useCase(
                authenticatedUserId = userId,
                currentPassword = currentPassword,
                newPassword = newPassword,
            )

            val failure = assertIs<ResultType.Failure<PasswordChangeError>>(result)

            assertEquals(
                expected = PasswordChangeError.NewPasswordMatchesCurrent,

                actual = failure.error,
            )

            /*
             * No mutation should be attempted when both plaintext
             * passwords are equal.
             */
            assertTrue(
                mutationRepository.passwordReplacementRequests.isEmpty()
            )

            assertTrue(
                mutationRepository.failedAttemptRequests.isEmpty()
            )

            assertTrue(
                mutationRepository.successfulAttemptRequests.isEmpty()
            )

            assertEquals(
                expected = 0,
                actual = passwordHasher.hashCalls,
            )
        } finally {
            currentPassword.fill(
                '\u0000'
            )

            newPassword.fill(
                '\u0000'
            )
        }
    }

    private fun activeUser(): User = User(
        id = UserId(
            UUID.randomUUID()
        ),

        accountName = "password-owner",
        emailAddress = "password-owner@example.com",
        firstName = "Password",
        lastName = "Owner",

        birthDate = LocalDate.parse(
            "2000-01-01"
        ),

        about = null,
        tagline = null,
        profileUrl = null,
        backgroundUrl = null,
        type = UserType.REGULAR,

        emailOptIn = false,
        isPrivate = true,
        isAnonymous = false,
        isActive = true,
        socialLinks = emptyList(),
        createdAt = Instant.parse(
            "2026-07-01T12:00:00Z"
        ),

        updatedAt = Instant.parse(
            "2026-07-01T12:00:00Z"
        ),
    )

    private fun credential(
        userId: UserId,
        failedLoginAttempts: Int = 0,
    ): AuthenticationCredential = AuthenticationCredential(
        userId = userId,

        passwordHash = "hash:current-password-123",
        passwordAlgorithm = PasswordHashAlgorithm.ARGON2ID,
        passwordUpdatedAt = Instant.parse(
            "2026-07-01T12:00:00Z"
        ),

        failedLoginAttempts = failedLoginAttempts,
        lockedUntil = null,
        sessionVersion = 1L,

        createdAt = Instant.parse(
            "2026-07-01T12:00:00Z"
        ),
        updatedAt = Instant.parse(
            "2026-07-01T12:00:00Z"
        ),
    )
}

/**
 * Deterministic password hasher used by the password-change tests.
 */
private class PasswordHasherFake : PasswordHasher {

    override val algorithm = PasswordHashAlgorithm.ARGON2ID

    var hashCalls: Int = 0
        private set

    override suspend fun hash(
        password: CharArray,
    ): String {
        hashCalls += 1

        return "hash:${password.concatToString()}"
    }

    override suspend fun verify(
        password: CharArray,
        encodedHash: String,
    ): Boolean = encodedHash == "hash:${password.concatToString()}"
}

/**
 * Supplies one persisted user to the use case.
 */
private class UserRepositoryFake(
    private val user: User,
) : UserRepository {

    override suspend fun getUserById(
        userId: UserId,
    ): ResultType<User, DataError> = if (user.id == userId) {
        ResultType.Success(
            user
        )
    } else {
        ResultType.Failure(
            DataError.NotFound
        )
    }

    override suspend fun getUsers(
        userType: UserType?,
    ): ResultType<List<User>, DataError> = error(
        "getUsers is not used by " + "ChangeCurrentUserPasswordUseCaseTest."
    )

    override suspend fun getUserByEmailAddress(
        emailAddress: String,
    ): ResultType<User, DataError> = error(
        "getUserByEmailAddress is not used by " + "ChangeCurrentUserPasswordUseCaseTest."
    )

    override suspend fun createUser(
        user: User,
    ): ResultType<Unit, DataError> = error(
        "createUser is not used by " + "ChangeCurrentUserPasswordUseCaseTest."
    )

    override suspend fun createUsers(
        users: List<User>,
    ): ResultType<Unit, DataError> = error(
        "createUsers is not used by " + "ChangeCurrentUserPasswordUseCaseTest."
    )

    override suspend fun updateUser(
        user: User,
    ): ResultType<Unit, DataError> = error(
        "updateUser is not used by " + "ChangeCurrentUserPasswordUseCaseTest."
    )

    override suspend fun deleteUserById(
        userId: UserId,
    ): ResultType<Unit, DataError> = error(
        "deleteUserById is not used by " + "ChangeCurrentUserPasswordUseCaseTest."
    )
}

/**
 * Read-only credential repository fake.
 *
 * Credential mutations are deliberately handled by the separate mutation
 * repository fake.
 */
private class CredentialRepositoryFake(
    private val storedCredential: AuthenticationCredential,
) : AuthenticationCredentialRepository {

    override suspend fun getCredentialByUserId(
        userId: UserId,
    ): ResultType<AuthenticationCredential, DataError> = if (storedCredential.userId == userId) {
        ResultType.Success(
            storedCredential
        )
    } else {
        ResultType.Failure(
            DataError.NotFound
        )
    }

    override suspend fun createCredential(
        credential: AuthenticationCredential,
    ): ResultType<Unit, DataError> = error(
        "createCredential is not used by ChangeCurrentUserPasswordUseCaseTest."
    )

    override suspend fun deleteCredentialByUserId(
        userId: UserId,
    ): ResultType<Unit, DataError> = error(
        "deleteCredentialByUserId is not used by ChangeCurrentUserPasswordUseCaseTest."
    )
}

/**
 * Captures one failed-login mutation command.
 */
private data class FailedAttemptRequest(
    val userId: UserId,
    val expectedPasswordHash: String,
    val expectedSessionVersion: Long,
    val attemptedAt: Timestamp,
)

/**
 * Captures one successful-login mutation command.
 */
private data class SuccessfulAttemptRequest(
    val userId: UserId,
    val expectedPasswordHash: String,
    val expectedSessionVersion: Long,
    val authenticatedAt: Timestamp,
)

/**
 * Captures one atomic password replacement command.
 */
private data class PasswordReplacementRequest(
    val userId: UserId,
    val expectedPasswordHash: String,
    val expectedSessionVersion: Long,
    val passwordHash: String,
    val passwordAlgorithm: PasswordHashAlgorithm,
    val passwordUpdatedAt: Timestamp,
)

/**
 * Configurable mutation repository fake that records every command.
 */
private class AuthenticationCredentialMutationRepositoryFake(
    var failedAttemptResult: ResultType<AuthenticationAttemptMutationResult, DataError> = ResultType.Success(
        AuthenticationAttemptMutationResult.Applied(
                sessionVersion = 1L
            )
    ),

    var successfulAttemptResult: ResultType<AuthenticationAttemptMutationResult, DataError> = ResultType.Success(
        AuthenticationAttemptMutationResult.Applied(
                sessionVersion = 1L
            )
    ),

    var passwordReplacementResult: ResultType<Long, DataError> = ResultType.Success(2L),
) : AuthenticationCredentialMutationRepository {

    val failedAttemptRequests = mutableListOf<FailedAttemptRequest>()

    val successfulAttemptRequests = mutableListOf<SuccessfulAttemptRequest>()

    val passwordReplacementRequests = mutableListOf<PasswordReplacementRequest>()

    override suspend fun recordFailedLoginAttempt(
        userId: UserId,
        expectedPasswordHash: String,
        expectedSessionVersion: Long,
        attemptedAt: Timestamp,
    ): ResultType<AuthenticationAttemptMutationResult, DataError> {
        failedAttemptRequests += FailedAttemptRequest(
            userId = userId,
            expectedPasswordHash = expectedPasswordHash,
            expectedSessionVersion = expectedSessionVersion,
            attemptedAt = attemptedAt,
        )

        return failedAttemptResult
    }

    override suspend fun recordSuccessfulLogin(
        userId: UserId,
        expectedPasswordHash: String,
        expectedSessionVersion: Long,
        authenticatedAt: Timestamp,
    ): ResultType<AuthenticationAttemptMutationResult, DataError> {
        successfulAttemptRequests += SuccessfulAttemptRequest(
            userId = userId,
            expectedPasswordHash = expectedPasswordHash,
            expectedSessionVersion = expectedSessionVersion,
            authenticatedAt = authenticatedAt,
        )

        return successfulAttemptResult
    }

    override suspend fun replacePasswordAndIncrementSessionVersion(
        userId: UserId,
        expectedPasswordHash: String,
        expectedSessionVersion: Long,
        passwordHash: String,
        passwordAlgorithm: PasswordHashAlgorithm,
        passwordUpdatedAt: Timestamp,
    ): ResultType<Long, DataError> {
        passwordReplacementRequests += PasswordReplacementRequest(
            userId = userId,
            expectedPasswordHash = expectedPasswordHash,
            expectedSessionVersion = expectedSessionVersion,
            passwordHash = passwordHash,
            passwordAlgorithm = passwordAlgorithm,
            passwordUpdatedAt = passwordUpdatedAt,
        )

        return passwordReplacementResult
    }
}