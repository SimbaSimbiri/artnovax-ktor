package com.simbiri.domain.policy.community

import com.simbiri.domain.model.community.Community
import com.simbiri.domain.util.DataError

/**
 * Contains pure business rules for creating and updating communities.
 *
 * This policy:
 * - does not know about HTTP DTOs;
 * - does not parse UUID strings;
 * - does not query the database;
 * - does not depend on Ktor or Exposed.
 */
object CommunityPolicy {

    private const val MIN_NAME_LENGTH = 3
    private const val MAX_NAME_LENGTH = 150
    private const val MAX_TAGLINE_LENGTH = 255
    private const val MAX_PROFILE_URL_LENGTH = 512
    private const val MAX_CHAT_BACKGROUND_URL_LENGTH = 512
    private const val MAX_CATEGORY_LENGTH = 100

    private const val MAX_SOCIAL_USERNAME_LENGTH = 80
    private const val MAX_SOCIAL_URL_LENGTH = 512

    /**
     * Returns the first validation error, or null when the community
     * satisfies all domain rules.
     */
    fun validateForUpsert(
        community: Community,
    ): DataError.ValidationError? {
        val normalizedName = community.name.trim()
        val normalizedDescription = community.description.trim()
        val normalizedTagline = community.tagline.trim()

        val blankSocialUsernameIndex =
            community.socialLinks.indexOfFirst { socialLink ->
                socialLink.username.isBlank()
            }

        val longSocialUsernameIndex =
            community.socialLinks.indexOfFirst { socialLink ->
                socialLink.username.length > MAX_SOCIAL_USERNAME_LENGTH
            }

        val blankSocialUrlIndex =
            community.socialLinks.indexOfFirst { socialLink ->
                socialLink.completeUrl.isBlank()
            }

        val longSocialUrlIndex =
            community.socialLinks.indexOfFirst { socialLink ->
                socialLink.completeUrl.length > MAX_SOCIAL_URL_LENGTH
            }

        val invalidPlatformIndex =
            community.socialLinks.indexOfFirst { socialLink ->
                socialLink.platform.id <= 0
            }

        val duplicateSocialLink =
            community.socialLinks
                .groupingBy { socialLink ->
                    socialLink.platform.id to
                            socialLink.username.trim().lowercase()
                }
                .eachCount()
                .entries
                .firstOrNull { (_, count) ->
                    count > 1
                }

        return when {
            normalizedName.length !in
                    MIN_NAME_LENGTH..MAX_NAME_LENGTH -> {
                validationError(
                    field = "name",
                    value = community.name,
                    reason = "Community name must contain between " +
                            "$MIN_NAME_LENGTH and $MAX_NAME_LENGTH characters."
                )
            }

            normalizedDescription.isBlank() -> {
                validationError(
                    field = "description",
                    value = community.description,
                    reason = "Community description is required."
                )
            }

            normalizedTagline.isBlank() -> {
                validationError(
                    field = "tagline",
                    value = community.tagline,
                    reason = "Community tagline is required."
                )
            }

            normalizedTagline.length > MAX_TAGLINE_LENGTH -> {
                validationError(
                    field = "tagline",
                    value = community.tagline,
                    reason = "Community tagline cannot exceed " +
                            "$MAX_TAGLINE_LENGTH characters."
                )
            }

            community.profileUrl != null &&
                    community.profileUrl.length > MAX_PROFILE_URL_LENGTH -> {
                validationError(
                    field = "profileUrl",
                    value = community.profileUrl,
                    reason = "Community profile URL cannot exceed " +
                            "$MAX_PROFILE_URL_LENGTH characters."
                )
            }

            community.chatBackgroundUrl != null &&
                    community.chatBackgroundUrl.length >
                    MAX_CHAT_BACKGROUND_URL_LENGTH -> {
                validationError(
                    field = "chatBackgroundUrl",
                    value = community.chatBackgroundUrl,
                    reason = "Community chat-background URL cannot exceed " +
                            "$MAX_CHAT_BACKGROUND_URL_LENGTH characters."
                )
            }

            community.category != null &&
                    community.category.length > MAX_CATEGORY_LENGTH -> {
                validationError(
                    field = "category",
                    value = community.category,
                    reason = "Community category cannot exceed " +
                            "$MAX_CATEGORY_LENGTH characters."
                )
            }

            community.memberCount < 0 -> {
                validationError(
                    field = "memberCount",
                    value = community.memberCount,
                    reason = "Community member count cannot be negative."
                )
            }

            invalidPlatformIndex >= 0 -> {
                validationError(
                    field = "socialLinks[$invalidPlatformIndex].platform.id",
                    value = community
                        .socialLinks[invalidPlatformIndex]
                        .platform
                        .id,
                    reason = "Social-platform ID must be a positive integer."
                )
            }

            blankSocialUsernameIndex >= 0 -> {
                validationError(
                    field = "socialLinks[$blankSocialUsernameIndex].username",
                    value = community
                        .socialLinks[blankSocialUsernameIndex]
                        .username,
                    reason = "Social-link username cannot be blank."
                )
            }

            longSocialUsernameIndex >= 0 -> {
                validationError(
                    field = "socialLinks[$longSocialUsernameIndex].username",
                    value = community
                        .socialLinks[longSocialUsernameIndex]
                        .username,
                    reason = "Social-link username cannot exceed " +
                            "$MAX_SOCIAL_USERNAME_LENGTH characters."
                )
            }

            blankSocialUrlIndex >= 0 -> {
                validationError(
                    field = "socialLinks[$blankSocialUrlIndex].completeUrl",
                    value = community
                        .socialLinks[blankSocialUrlIndex]
                        .completeUrl,
                    reason = "Social-link URL cannot be blank."
                )
            }

            longSocialUrlIndex >= 0 -> {
                validationError(
                    field = "socialLinks[$longSocialUrlIndex].completeUrl",
                    value = community
                        .socialLinks[longSocialUrlIndex]
                        .completeUrl,
                    reason = "Social-link URL cannot exceed " +
                            "$MAX_SOCIAL_URL_LENGTH characters."
                )
            }

            duplicateSocialLink != null -> {
                val platformId = duplicateSocialLink.key.first
                val username = duplicateSocialLink.key.second

                validationError(
                    field = "socialLinks",
                    value = "platformId=$platformId, username=$username",
                    reason = "A community cannot contain duplicate social " +
                            "links for the same platform and username."
                )
            }

            else -> null
        }
    }

    private fun validationError(
        field: String,
        value: Any?,
        reason: String,
    ): DataError.ValidationError =
        DataError.ValidationError(
            message = "Community validation failed. " +
                    "field=$field, value=$value. $reason"
        )
}