package com.simbiri.domain.model.community

enum class JoinPermission(val code: Int) {
    AUTO(0),
    APPROVAL(1);  // requires moderator/owner approval

    companion object {
        fun fromCode(code: Int): JoinPermission =
            entries.firstOrNull { it.code == code }
                ?: error("Unknown join permission code: $code")
    }
}
