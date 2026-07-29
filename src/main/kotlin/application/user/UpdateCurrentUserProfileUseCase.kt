package com.simbiri.application.user

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.user.CurrentUserProfileUpdate
import com.simbiri.domain.model.user.User
import com.simbiri.domain.policy.user.UserPolicy
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.time.Clock
import java.time.LocalDate

/**
 * Updates profile information belonging to the authenticated user.
 *
 * The persisted user supplies all identity, authorization, and lifecycle
 * fields. Client cannot change those fields through this operation.
 */
class UpdateCurrentUserProfileUseCase(
    private val userRepository: UserRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(
        authenticatedUserId: UserId,
        profileUpdate: CurrentUserProfileUpdate,
    ): ResultType<User, DataError> {
        val currentUser = when (val result = userRepository.getUserById(
            authenticatedUserId
        )) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> return ResultType.Failure(
                result.error
            )
        }

        val persistedUserId = currentUser.id ?: return ResultType.Failure(
            DataError.UnknownError(
                cause = "Persisted current user is missing its ID."
            )
        )

        if (persistedUserId != authenticatedUserId) {
            return ResultType.Failure(
                DataError.UnknownError(
                    cause = "Current-user repository result does not match the authenticated user ID."
                )
            )
        }

        if (!currentUser.isActive) {
            return ResultType.Failure(
                DataError.Forbidden(
                    message = "Current-user profile update failed. The authenticated account is inactive."
                )
            )
        }

        /*
         * Identity, authorization, and lifecycle fields are copied from the
         * persisted user rather than accepted from the client.
         */
        val updatedUser = currentUser.copy(
            firstName = profileUpdate.firstName,
            lastName = profileUpdate.lastName,
            birthDate = profileUpdate.birthDate,

            about = profileUpdate.about,
            tagline = profileUpdate.tagline,

            profileUrl = profileUpdate.profileUrl,
            backgroundUrl = profileUpdate.backgroundUrl,

            emailOptIn = profileUpdate.emailOptIn,
            isPrivate = profileUpdate.isPrivate,
            isAnonymous = profileUpdate.isAnonymous,
            socialLinks = profileUpdate.socialLinks,
        )

        UserPolicy.validateForUpsert(
                user = updatedUser,
                today = LocalDate.now(clock),
            )?.let { error ->
                return ResultType.Failure(error)
            }

        when (val updateResult = userRepository.updateUser(
            updatedUser
        )) {
            is ResultType.Success -> Unit

            is ResultType.Failure -> return ResultType.Failure(
                updateResult.error
            )
        }

        // Returns reloaded persisted changes
        return userRepository.getUserById(
            authenticatedUserId
        )
    }
}
