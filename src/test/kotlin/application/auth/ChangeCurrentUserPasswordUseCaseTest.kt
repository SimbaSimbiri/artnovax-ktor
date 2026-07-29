package com.simbiri.application.auth

import com.simbiri.domain.model.auth.AuthenticationCredential
import com.simbiri.domain.model.auth.PasswordChangeError
import com.simbiri.domain.model.auth.PasswordHashAlgorithm
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.user.User
import com.simbiri.domain.model.user.UserType
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

        val credentialRepository = CredentialRepositoryFake(
            storedCredential = credential(
                userId = requireNotNull(
                    user.id
                ),

                failedLoginAttempts = 3,
            )
        )

        val passwordHasher = PasswordHasherFake()

        val useCase = ChangeCurrentUserPasswordUseCase(
            userRepository = UserRepositoryFake(user),
            credentialRepository = credentialRepository,
            passwordHasher = passwordHasher,
            clock = clock,
        )

        val currentPassword = "current-password-123".toCharArray()

        val newPassword = "replacement-password-123".toCharArray()

        try {
            val result = useCase(
                authenticatedUserId = requireNotNull(
                    user.id
                ),
                currentPassword = currentPassword,
                newPassword = newPassword,
            )

            assertIs<ResultType.Success<Unit>>(result)

            val updatedCredential = credentialRepository.storedCredential

            assertEquals(
                expected = "hash:replacement-password-123",
                actual = updatedCredential.passwordHash,
            )

            assertEquals(
                expected = changedAt,
                actual = updatedCredential.passwordUpdatedAt,
            )

            assertEquals(
                expected = 0,
                actual = updatedCredential.failedLoginAttempts,
            )

            assertEquals(
                expected = null,
                actual = updatedCredential.lockedUntil,
            )

            assertEquals(
                expected = 1,
                actual = passwordHasher.hashCalls,
            )
        } finally {
            currentPassword.fill('\u0000')
            newPassword.fill('\u0000')
        }
    }

    @Test
    fun `incorrect current password records failed attempt`() = runBlocking {
        val user = activeUser()

        val credentialRepository = CredentialRepositoryFake(
            storedCredential = credential(
                userId = requireNotNull(
                    user.id
                )
            )
        )

        val passwordHasher = PasswordHasherFake()

        val useCase = ChangeCurrentUserPasswordUseCase(
            userRepository = UserRepositoryFake(user),
            credentialRepository = credentialRepository,
            passwordHasher = passwordHasher,
            clock = clock,
        )

        val result = useCase(
            authenticatedUserId = requireNotNull(user.id),
            currentPassword = "incorrect-password".toCharArray(),
            newPassword = "replacement-password-123".toCharArray(),
        )

        val failure = assertIs<ResultType.Failure<PasswordChangeError>>(result)

        assertEquals(
            expected = PasswordChangeError.InvalidCurrentPassword,
            actual = failure.error,
        )

        assertEquals(
            expected = 1,
            actual = credentialRepository.storedCredential.failedLoginAttempts,
        )

        assertEquals(
            expected = 0,
            actual = passwordHasher.hashCalls,
        )
    }

    @Test
    fun `new password must differ from current password`() = runBlocking {
        val user = activeUser()

        val credentialRepository = CredentialRepositoryFake(
            storedCredential = credential(
                userId = requireNotNull(
                    user.id
                )
            )
        )

        val passwordHasher = PasswordHasherFake()

        val useCase = ChangeCurrentUserPasswordUseCase(
            userRepository = UserRepositoryFake(user),
            credentialRepository = credentialRepository,
            passwordHasher = passwordHasher,
            clock = clock,
        )

        val password = "current-password-123".toCharArray()

        val result = useCase(
            authenticatedUserId = requireNotNull(user.id),
            currentPassword = password,
            newPassword = password.copyOf(),
        )

        val failure = assertIs<ResultType.Failure<PasswordChangeError>>(result)

        assertEquals(
            expected = PasswordChangeError.NewPasswordMatchesCurrent,
            actual = failure.error,
        )

        assertEquals(
            expected = 0,
            actual = credentialRepository.updateCalls,
        )
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

        createdAt = Instant.parse(
            "2026-07-01T12:00:00Z"
        ),

        updatedAt = Instant.parse(
            "2026-07-01T12:00:00Z"
        ),
    )
}

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

private class UserRepositoryFake(
    private val user: User,
) : UserRepository {

    override suspend fun getUserById(
        userId: UserId,
    ): ResultType<User, DataError> = if (user.id == userId) {
        ResultType.Success(user)
    } else {
        ResultType.Failure(
            DataError.NotFound
        )
    }

    override suspend fun getUsers(
        userType: UserType?,
    ): ResultType<List<User>, DataError> = error("Not used by this test.")

    override suspend fun getUserByEmailAddress(
        emailAddress: String,
    ): ResultType<User, DataError> = error("Not used by this test.")

    override suspend fun createUser(
        user: User,
    ): ResultType<Unit, DataError> = error("Not used by this test.")

    override suspend fun createUsers(
        users: List<User>,
    ): ResultType<Unit, DataError> = error("Not used by this test.")

    override suspend fun updateUser(
        user: User,
    ): ResultType<Unit, DataError> = error("Not used by this test.")

    override suspend fun deleteUserById(
        userId: UserId,
    ): ResultType<Unit, DataError> = error("Not used by this test.")
}

private class CredentialRepositoryFake(
    var storedCredential: AuthenticationCredential,
) : AuthenticationCredentialRepository {

    var updateCalls: Int = 0
        private set

    override suspend fun getCredentialByUserId(
        userId: UserId,
    ): ResultType<
            AuthenticationCredential,
            DataError,
            > = if (storedCredential.userId == userId) {
        ResultType.Success(
            storedCredential
        )
    } else {
        ResultType.Failure(
            DataError.NotFound
        )
    }

    override suspend fun updateCredential(
        credential: AuthenticationCredential,
    ): ResultType<Unit, DataError> {
        updateCalls += 1
        storedCredential = credential

        return ResultType.Success(Unit)
    }

    override suspend fun createCredential(
        credential: AuthenticationCredential,
    ): ResultType<Unit, DataError> = error("Not used by this test.")

    override suspend fun deleteCredentialByUserId(
        userId: UserId,
    ): ResultType<Unit, DataError> = error("Not used by this test.")
}
