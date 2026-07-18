package com.simbiri.domain.model.community

enum class CommunityParticipantRole {
    OWNER,
    MEMBER,
    MODERATOR;

    /**
     * prevents presentation mappers from relying on unsafe valueOf calls
     */
    companion object {

        fun fromNameOrNull(
            value: String,
        ): CommunityParticipantRole? {
            val normalizedValue = value.trim()

            return entries.firstOrNull { role ->
                role.name.equals(
                    other = normalizedValue,
                    ignoreCase = true,
                )
            }
        }

        fun fromName(
            value: String,
        ): CommunityParticipantRole =
            fromNameOrNull(value)
                ?: throw IllegalArgumentException(
                    "Unsupported community participant role: '$value'."
                )
    }
}