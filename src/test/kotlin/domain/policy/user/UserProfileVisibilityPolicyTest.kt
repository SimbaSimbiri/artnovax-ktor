package com.simbiri.domain.policy.user

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.user.User
import com.simbiri.domain.model.user.UserType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserProfileVisibilityPolicyTest {

    @Test
    fun `active non-private user is publicly visible`() {
        assertTrue(
            UserProfileVisibilityPolicy.isPubliclyVisible(
                    user()
                )
        )
    }

    @Test
    fun `private user is not publicly visible`() {
        assertFalse(
            UserProfileVisibilityPolicy.isPubliclyVisible(
                    user().copy(
                        isPrivate = true
                    )
                )
        )
    }

    @Test
    fun `inactive user is not publicly visible`() {
        assertFalse(
            UserProfileVisibilityPolicy.isPubliclyVisible(
                    user().copy(
                        isActive = false
                    )
                )
        )
    }

    @Test
    fun `anonymous user does not expose real name`() {
        assertFalse(
            UserProfileVisibilityPolicy.canExposeRealName(
                    user().copy(
                        isAnonymous = true
                    )
                )
        )
    }

    private fun user(): User = User(
        id = UserId(
            UUID.randomUUID()
        ),

        accountName = "public-user",
        emailAddress = "public@example.com",

        firstName = "Public",
        lastName = "User",

        birthDate = LocalDate.parse(
            "2000-01-01"
        ),

        about = null,
        tagline = null,
        profileUrl = null,
        backgroundUrl = null,

        type = UserType.REGULAR,

        emailOptIn = false,
        isPrivate = false,
        isAnonymous = false,
        isActive = true,

        socialLinks = emptyList(),

        createdAt = Instant.parse(
            "2026-07-28T12:00:00Z"
        ),

        updatedAt = Instant.parse(
            "2026-07-28T12:00:00Z"
        ),
    )
}
