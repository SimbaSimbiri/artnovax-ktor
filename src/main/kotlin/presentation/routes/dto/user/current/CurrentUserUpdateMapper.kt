package com.simbiri.presentation.routes.dto.user.current

import com.simbiri.domain.model.social.SocialLink
import com.simbiri.domain.model.user.CurrentUserProfileUpdate
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.routes.dto.social.toDomain
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Parses a current-user profile request into its application command.
 */
fun CurrentUserUpdateRequestDto.toCurrentUserProfileUpdate(): ResultType<
        CurrentUserProfileUpdate,
        DataError,
        > {
    val parsedBirthDate = try {
        LocalDate.parse(
            birthDate
        )
    } catch (_: DateTimeParseException) {
        return ResultType.Failure(
            DataError.ValidationError(
                message = "Current-user profile update failed. birthDate must use ISO-8601 format YYYY-MM-DD."
            )
        )
    }

    val parsedSocialLinks = try {
        socialLinks.map { socialLink ->
            socialLink.toDomain()!!
        }
    } catch (_: IllegalArgumentException) {
        return ResultType.Failure(
            DataError.ValidationError(
                message = "Current-user profile update failed. One or more social links are invalid."
            )
        )
    }

    return ResultType.Success(
        CurrentUserProfileUpdate(
            firstName = firstName,
            lastName = lastName,
            birthDate = parsedBirthDate,

            about = about,
            tagline = tagline,

            profileUrl = profileImageUrl,
            backgroundUrl = backgroundImageUrl,

            emailOptIn = emailOptIn,
            isPrivate = isPrivate,
            isAnonymous = isAnonymous,
            socialLinks = parsedSocialLinks,
        )
    )
}
