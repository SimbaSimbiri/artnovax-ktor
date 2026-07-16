package com.simbiri.domain.policy.user

import com.simbiri.domain.model.user.User
import com.simbiri.domain.util.DataError
import java.time.LocalDate

/**
 * Contains pure business rules that determine whether a User
 * is valid for creation or update.
 *
 * This policy:
 * - does not know about Ktor request DTOs;
 * - does not query the database;
 * - does not use Exposed;
 * - can be reused by HTTP, bulk import, tests, and future background jobs.
 */
object UserPolicy {

    private const val MIN_ACCOUNT_NAME_LENGTH = 3
    private const val MAX_ACCOUNT_NAME_LENGTH = 50

    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /**
     * Returns the first domain validation error, or null when the
     * user satisfies all upsert-related business rules.
     *
     * The caller supplies [today] instead of this policy calling
     * LocalDate.now() directly to keep validation deterministic
     * and easy to test.
     */
    fun validateForUpsert(
        user: User,
        today: LocalDate,
    ): DataError.ValidationError? {
        val accountName = user.accountName.trim()
        val emailAddress = user.emailAddress.trim()

        return when {
            accountName.length !in
                    MIN_ACCOUNT_NAME_LENGTH..MAX_ACCOUNT_NAME_LENGTH -> {
                validationError(
                    field = "accountName",
                    value = user.accountName,
                    reason = "Account name must contain between " +
                            "$MIN_ACCOUNT_NAME_LENGTH and " +
                            "$MAX_ACCOUNT_NAME_LENGTH characters."
                )
            }

            emailAddress.isBlank() -> {
                validationError(
                    field = "emailAddress",
                    value = user.emailAddress,
                    reason = "Email address is required."
                )
            }

            !EMAIL_REGEX.matches(emailAddress) -> {
                validationError(
                    field = "emailAddress",
                    value = user.emailAddress,
                    reason = "Email address has an invalid format."
                )
            }

            user.firstName.isBlank() -> {
                validationError(
                    field = "firstName",
                    value = user.firstName,
                    reason = "First name is required."
                )
            }

            user.lastName.isBlank() -> {
                validationError(
                    field = "lastName",
                    value = user.lastName,
                    reason = "Last name is required."
                )
            }

            user.birthDate.isAfter(today) -> {
                validationError(
                    field = "birthDate",
                    value = user.birthDate.toString(),
                    reason = "Birth date cannot be in the future."
                )
            }

            !user.canExposeSocialLinks && user.socialLinks.isNotEmpty() -> {
                validationError(
                    field = "socialLinks",
                    value = "userType=${user.type}, " +
                            "socialLinks.size=${user.socialLinks.size}",
                    reason = "User type '${user.type}' is not allowed " +
                            "to expose social links."
                )
            }

            user.socialLinks.any { socialLink ->
                socialLink.username.isBlank()
            } -> {
                val invalidIndex = user.socialLinks.indexOfFirst { socialLink ->
                    socialLink.username.isBlank()
                }

                validationError(
                    field = "socialLinks[$invalidIndex].username",
                    value = user.socialLinks[invalidIndex].username,
                    reason = "Social-link username cannot be blank."
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
            message = "User validation failed. " +
                    "field=$field, value=$value. $reason"
        )
}