package com.simbiri.presentation.validator

import com.simbiri.presentation.routes.dto.user.UserUpsertDto
import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import java.time.LocalDate
import java.time.format.DateTimeParseException

private val ALLOWED_USER_TYPES = 0..5
private val ALLOWED_SOCIAL_PLATFORM_IDS = setOf(1, 2, 3, 4, 5)
private fun canTypeHaveSocialLinks(type: Int): Boolean = type == 3 || type == 4 || type == 5

fun RequestValidationConfig.validateUserUpsert() {
    validate<UserUpsertDto> { dto ->

        val birthDate = try {
            LocalDate.parse(dto.birthDate)
        } catch (e: DateTimeParseException) {
            return@validate ValidationResult.Invalid(
                "birthDate must be in ISO-8601 format (yyyy-MM-dd)."
            )
        }

        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")

        when {
            dto.accountName.isBlank() || dto.accountName.length !in 3..50 -> {
                ValidationResult.Invalid("accountName must be between 3 and 50 characters.")
            }

            dto.emailAddress.isBlank() || !dto.emailAddress.trim().matches(emailRegex) -> {
                ValidationResult.Invalid(reason = "User email is invalid")
            }

            dto.firstName.isBlank() -> {
                ValidationResult.Invalid("firstName is required.")
            }

            dto.lastName.isBlank() -> {
                ValidationResult.Invalid("lastName is required.")
            }

            birthDate.isAfter(LocalDate.now()) -> {
                ValidationResult.Invalid("birthDate cannot be in the future.")
            }

            dto.type !in ALLOWED_USER_TYPES -> {
                ValidationResult.Invalid("type must be a valid user type code.")
            }

            // no socials for regular / moderators/ group owners
            !canTypeHaveSocialLinks(dto.type) && dto.socialLinks.isNotEmpty() -> {
                ValidationResult.Invalid(
                    "Only psychologists, admin/exec users, and devs may include social links."
                )
            }

            // for types that are allowed to have socials, validate each link
            canTypeHaveSocialLinks(dto.type) && dto.socialLinks.any { it.username.isBlank() } -> {
                ValidationResult.Invalid("Each social link must have a non-empty username.")
            }

            canTypeHaveSocialLinks(dto.type) &&
                    dto.socialLinks.any { it.platformId !in ALLOWED_SOCIAL_PLATFORM_IDS } -> {
                ValidationResult.Invalid("One or more Social Links are not in Platform registry.")
            }

            else -> ValidationResult.Valid
        }
    }
}
