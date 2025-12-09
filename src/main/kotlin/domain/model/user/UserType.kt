package com.simbiri.domain.model.user

enum class UserType (val code: Int) {
    REGULAR(0),
    ADMIN_EXEC(1),
    GROUP_MODERATOR(2),
    PSYCHOLOGIST(3),
    EVENTS_MODERATOR(4),
    DEV(5);

    companion object {
        fun fromCode(code: Int): UserType =
            entries.firstOrNull() { it.code == code } ?: error("Unknown user type: $code")
    }
}