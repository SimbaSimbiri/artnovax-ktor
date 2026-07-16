package com.simbiri.domain.model.user

enum class UserType(val code: Int) {
    REGULAR(0),
    POST_MODERATOR(1),
    EVENTS_MODERATOR(2),
    PSYCHOLOGIST(3),
    ADMIN_EXEC(4),
    DEV(5);

    /**
     * Defines if this user type may publicly expose social links
     * To be used as a single source of truth in every package
     */
    val canExposeSocialLinks: Boolean
        get() = this in SOCIAL_LINK_CAPABLE_TYPES

    companion object {
        private val SOCIAL_LINK_CAPABLE_TYPES: Set<UserType> = setOf(PSYCHOLOGIST, ADMIN_EXEC, DEV)

        /**
         * Returns matching user type or null
         * To be used at DTO and request validation for invalid type handling without exception
         */

        fun fromCodeOrNull(code: Int): UserType? = entries.firstOrNull { userType -> userType.code == code }

        /**
         * Returns matching user type after code has been validated
         */
        fun fromCode(code: Int): UserType =
            fromCodeOrNull(code) ?: throw IllegalArgumentException("Unsupported user type code: $code")
    }
}