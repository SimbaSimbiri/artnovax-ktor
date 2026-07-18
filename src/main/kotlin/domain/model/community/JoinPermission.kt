package com.simbiri.domain.model.community

enum class JoinPermission(val code: Int) {
    AUTO(0),
    APPROVAL(1);  // requires moderator/owner approval

    companion object {

        fun fromCodeOrNull(
            code: Int,
        ): JoinPermission? =
            entries.firstOrNull { permission ->
                permission.code == code
            }

        fun fromCode(
            code: Int,
        ): JoinPermission =
            fromCodeOrNull(code)
                ?: throw IllegalArgumentException(
                    "Unsupported community join-permission code: $code."
                )
    }
}
