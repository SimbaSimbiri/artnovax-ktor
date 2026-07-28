package com.simbiri.domain.model.auth

import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.user.User
import com.simbiri.domain.model.user.UserType

/**
 * Contains the fully validated and hashed material required to create one
 * self-registered ArtNovaX account.
 *
 */
data class UserRegistration(
    val user: User,
    val passwordHash: String,
    val passwordAlgorithm: PasswordHashAlgorithm,
    val passwordUpdatedAt: Timestamp,
) {

    init {
        require(user.id == null) {
            "A registration user must not already contain an ID."
        }

        require(
            user.createdAt == null && user.updatedAt == null
        ) {
            "A registration user must not contain persistence timestamps."
        }

        require(user.type == UserType.REGULAR) {
            "Public registration may only create REGULAR users."
        }

        require(user.socialLinks.isEmpty()) {
            "Public registration must not create social links."
        }

        require(passwordHash.isNotBlank()) {
            "Registration passwordHash must not be blank."
        }
    }

    /**
     * Prevents the encoded password hash from appearing in application logs.
     */
    override fun toString(): String =
        "UserRegistration(user=$user, passwordHash=<redacted>, passwordAlgorithm=$passwordAlgorithm, " +
                "passwordUpdatedAt=$passwordUpdatedAt)"
}
