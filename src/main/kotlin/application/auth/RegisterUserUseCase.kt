package com.simbiri.application.auth

import com.simbiri.domain.model.auth.UserRegistration
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.user.User
import com.simbiri.domain.model.user.UserType
import com.simbiri.domain.policy.auth.PasswordPolicy
import com.simbiri.domain.policy.user.UserPolicy
import com.simbiri.domain.repository.UserRegistrationRepository
import com.simbiri.domain.security.PasswordHasher
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/**
 * Creates one public ArtNovaX account and its initial password credential.
 *
 * The caller owns the supplied password array and must clear it after this
 * operation completes.
 */
class RegisterUserUseCase(
    private val registrationRepository: UserRegistrationRepository,
    private val passwordHasher: PasswordHasher,
    private val clock: Clock,
) {

    suspend operator fun invoke(
        user: User,
        password: CharArray,
    ): ResultType<UserId, DataError> {
        if (user.id != null || user.createdAt != null || user.updatedAt != null) {
            return ResultType.Failure(
                DataError.ValidationError(
                    message = "Registration failed. A new user must not contain an ID or persistence timestamps."
                )
            )
        }

        /*
         * Public callers must never be able to select moderator,
         * psychologist, administrator, or developer privileges.
         */
        if (user.type != UserType.REGULAR) {
            return ResultType.Failure(
                DataError.Forbidden(
                    message = "Public registration may only create REGULAR users."
                )
            )
        }

        if (user.socialLinks.isNotEmpty()) {
            return ResultType.Failure(
                DataError.ValidationError(
                    message = "Public registration cannot create social links."
                )
            )
        }

        UserPolicy.validateForUpsert(
                user = user,
                today = LocalDate.now(clock),
            )?.let { error ->
                return ResultType.Failure(error)
            }

        PasswordPolicy.validateForCredentialCreation(
                password
            )?.let { error ->
                return ResultType.Failure(error)
            }

        val workingPassword = password.copyOf()

        val encodedPasswordHash = try {
            passwordHasher.hash(
                workingPassword
            )
        } catch (_: Exception) {
            return ResultType.Failure(
                DataError.UnknownError(
                    cause = "Password hashing failed."
                )
            )
        } finally {
            workingPassword.fill('\u0000')
        }

        val registeredAt = Instant.now(clock)

        return registrationRepository.register(
            UserRegistration(
                user = user,
                passwordHash = encodedPasswordHash,
                passwordAlgorithm = passwordHasher.algorithm,
                passwordUpdatedAt = registeredAt,
            )
        )
    }
}
