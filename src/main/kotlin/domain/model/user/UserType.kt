package com.simbiri.domain.model.user

enum class UserType(
    val code: Int,
) {
    REGULAR(0),
    POST_MODERATOR(1),
    EVENTS_MODERATOR(2),
    PSYCHOLOGIST(3),
    ADMIN_EXEC(4),
    DEV(5);

    /**
     * Whether this user type may publicly expose social links.
     */
    val canExposeSocialLinks: Boolean
        get() = this in SOCIAL_LINK_CAPABLE_TYPES

    /**
     * Whether this user type may create and edit therapy-content
     * drafts.
     */
    val canAuthorTherapyContent: Boolean
        get() = this in THERAPY_CONTENT_AUTHOR_TYPES

    /**
     * Whether this user type may review submitted therapy content.
     */
    val canReviewTherapyContent: Boolean
        get() = this in THERAPY_CONTENT_REVIEWER_TYPES

    /**
     * Whether this user type may make reviewed therapy content
     * available to users.
     */
    val canPublishTherapyContent: Boolean
        get() = this in THERAPY_CONTENT_PUBLISHER_TYPES

    companion object {
        private val SOCIAL_LINK_CAPABLE_TYPES: Set<UserType> =
            setOf(
                PSYCHOLOGIST,
                ADMIN_EXEC,
                DEV,
            )

        private val THERAPY_CONTENT_AUTHOR_TYPES: Set<UserType> =
            setOf(
                PSYCHOLOGIST,
                ADMIN_EXEC,
                DEV,
            )

        private val THERAPY_CONTENT_REVIEWER_TYPES: Set<UserType> =
            setOf(
                ADMIN_EXEC,
                DEV,
            )

        private val THERAPY_CONTENT_PUBLISHER_TYPES: Set<UserType> =
            setOf(
                ADMIN_EXEC,
                DEV,
            )

        /**
         * Returns the matching type or null when the code is unsupported.
         */
        fun fromCodeOrNull(
            code: Int,
        ): UserType? =
            entries.firstOrNull { userType ->
                userType.code == code
            }

        /**
         * Returns the matching type after the code has already been
         * validated.
         */
        fun fromCode(
            code: Int,
        ): UserType =
            fromCodeOrNull(code)
                ?: throw IllegalArgumentException(
                    "Unsupported user type code: $code"
                )
    }
}