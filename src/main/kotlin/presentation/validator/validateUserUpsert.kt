package com.simbiri.presentation.validator

import com.simbiri.domain.model.user.UserType
import com.simbiri.presentation.routes.dto.user.UserUpsertDto
import io.ktor.server.plugins.requestvalidation.*
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Validates if HTTP request can be converted to user domain object
 *
 * Business rules will later be enforced in UserPolicy
 */

fun RequestValidationConfig.validateUserUpsert() {
    validate<UserUpsertDto> { userUpsertDto ->
        validateUserRequestShape(userUpsertDto)?.let(ValidationResult::Invalid) ?: ValidationResult.Valid
    }

    /*
     * Registers the bulk request explicitly so bulk operations cannot
     * bypass transport-level validation.
     */
    validate<List<UserUpsertDto>> { usersDtoReceived ->
        val firstError = usersDtoReceived
            .withIndex()
            .firstNotNullOfOrNull { (index, dto) ->
                validateUserRequestShape(dto)?.let { reason ->
                    "users[$index]: $reason"
                }
            }

        firstError
            ?.let(ValidationResult::Invalid)
            ?: ValidationResult.Valid
    }
}


/**
 * Checks only request-representation concerns.
 *
 * Does not enforce domain rules on the received upsert fields.
 */
private fun validateUserRequestShape(userUpsertDto: UserUpsertDto): String? {
    try {
        LocalDate.parse(userUpsertDto.birthDate)
    } catch (_: DateTimeParseException) {
        return "birthDate must use ISO-8601 format: yyyy-MM-dd."
    }

    if (UserType.fromCodeOrNull(userUpsertDto.type) == null) {
        return "type '${userUpsertDto.type}' is not a supported user type code."
    }

    val invalidPlatformIndex = userUpsertDto.socialLinks.indexOfFirst { socialLink ->
        socialLink.platformId <= 0
    }

    if (invalidPlatformIndex >= 0) {
        val invalidPlatformId =
            userUpsertDto.socialLinks[invalidPlatformIndex].platformId

        return "socialLinks[$invalidPlatformIndex].platformId must be " +
                "a positive integer, but was $invalidPlatformId."
    }

    return null
}
