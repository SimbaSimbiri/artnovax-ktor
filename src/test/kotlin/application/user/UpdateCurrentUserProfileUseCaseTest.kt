package com.simbiri.application.user

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.user.CurrentUserProfileUpdate
import com.simbiri.domain.model.user.User
import com.simbiri.domain.model.user.UserType
import com.simbiri.domain.repository.UserRepository
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

class UpdateCurrentUserProfileUseCaseTest {

    private val clock = Clock.fixed(
        Instant.parse(
            "2026-07-28T18:00:00Z"
        ),
        ZoneOffset.UTC,
    )

    @Test
    fun `profile update preserves identity role and lifecycle fields`() = runBlocking {
        val originalUser = persistedUser()

        val repository = CurrentUserRepositoryFake(
            storedUser = originalUser
        )

        val useCase = UpdateCurrentUserProfileUseCase(
            userRepository = repository,
            clock = clock,
        )

        val result = useCase(
            authenticatedUserId = requireNotNull(
                originalUser.id
            ),

            profileUpdate = CurrentUserProfileUpdate(
                firstName = "Updated",
                lastName = "Profile",
                birthDate = LocalDate.parse(
                    "1999-05-20"
                ),

                about = "Updated biography",
                tagline = "Updated tagline",

                profileUrl = "profiles/updated.webp",
                backgroundUrl = "backgrounds/updated.webp",

                emailOptIn = true,
                isPrivate = false,
                isAnonymous = true,

                socialLinks = emptyList(),
            ),
        )

        val success = assertIs<ResultType.Success<User>>(result)

        val updatedUser = success.data

        assertEquals(
            expected = originalUser.id,
            actual = updatedUser.id,
        )

        assertEquals(
            expected = originalUser.accountName,
            actual = updatedUser.accountName,
        )

        assertEquals(
            expected = originalUser.emailAddress,
            actual = updatedUser.emailAddress,
        )

        assertEquals(
            expected = originalUser.type,
            actual = updatedUser.type,
        )

        assertEquals(
            expected = originalUser.isActive,
            actual = updatedUser.isActive,
        )

        assertEquals(
            expected = "Updated",
            actual = updatedUser.firstName,
        )

        assertEquals(
            expected = 1,
            actual = repository.updateCalls,
        )
    }

    @Test
    fun `inactive authenticated user cannot update profile`() = runBlocking {
        val inactiveUser = persistedUser().copy(
            isActive = false
        )

        val repository = CurrentUserRepositoryFake(
            storedUser = inactiveUser
        )

        val useCase = UpdateCurrentUserProfileUseCase(
            userRepository = repository,

            clock = clock,
        )

        val result = useCase(
            authenticatedUserId = requireNotNull(
                inactiveUser.id
            ),
            profileUpdate = CurrentUserProfileUpdate(
                firstName = inactiveUser.firstName,
                lastName = inactiveUser.lastName,
                birthDate = inactiveUser.birthDate,

                about = inactiveUser.about,
                tagline = inactiveUser.tagline,

                profileUrl = inactiveUser.profileUrl,
                backgroundUrl = inactiveUser.backgroundUrl,

                emailOptIn = inactiveUser.emailOptIn,
                isPrivate = inactiveUser.isPrivate,
                isAnonymous = inactiveUser.isAnonymous,
                socialLinks = inactiveUser.socialLinks,
            ),
        )

        assertTrue(
            result is ResultType.Failure
        )

        assertEquals(
            expected = 0,
            actual = repository.updateCalls,
        )
    }

    private fun persistedUser(): User = User(
        id = UserId(
            UUID.randomUUID()
        ),
        accountName = "profile-owner",
        emailAddress = "owner@example.com",
        firstName = "Profile",
        lastName = "Owner",
        birthDate = LocalDate.parse(
            "2000-01-01"
        ),

        about = null,
        tagline = null,
        profileUrl = null,
        backgroundUrl = null,

        type = UserType.ADMIN_EXEC,

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
}

/**
 * Minimal mutable repository fake for current-user profile tests.
 */
private class CurrentUserRepositoryFake(
    private var storedUser: User,
) : UserRepository {

    var updateCalls: Int = 0
        private set

    override suspend fun getUserById(
        userId: UserId,
    ): ResultType<User, DataError> = if (storedUser.id == userId) {
        ResultType.Success(
            storedUser
        )
    } else {
        ResultType.Failure(
            DataError.NotFound
        )
    }

    override suspend fun updateUser(
        user: User,
    ): ResultType<Unit, DataError> {
        updateCalls += 1
        storedUser = user

        return ResultType.Success(Unit)
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

    override suspend fun deleteUserById(
        userId: UserId,
    ): ResultType<Unit, DataError> = error("Not used by this test.")
}
