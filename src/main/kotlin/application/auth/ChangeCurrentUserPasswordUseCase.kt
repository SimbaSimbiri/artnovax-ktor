package com.simbiri.application.auth

import com.simbiri.domain.model.auth.PasswordChangeError
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.policy.auth.AuthenticationAttemptPolicy
import com.simbiri.domain.policy.auth.PasswordPolicy
import com.simbiri.domain.repository.AuthenticationCredentialRepository
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.security.PasswordHasher
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.time.Clock
import java.time.Instant

/**
 * Changes the password belonging to an authenticated active user.
 *
 * The existing password must be verified before the credential is replaced.
 * The caller owns both supplied arrays and must clear them after invocation.
 */
class ChangeCurrentUserPasswordUseCase(
    private val userRepository: UserRepository,
    private val credentialRepository: AuthenticationCredentialRepository,
    private val passwordHasher: PasswordHasher,
    private val clock: Clock,
) {

    suspend operator fun invoke(
        authenticatedUserId: UserId,
        currentPassword: CharArray,
        newPassword: CharArray,
    ): ResultType<Unit, PasswordChangeError> {
        if (currentPassword.isEmpty()) {
            return failure(
                PasswordChangeError.InvalidCurrentPassword
            )
        }

        PasswordPolicy.validateForCredentialCreation(
                newPassword
            )?.let { error ->
                return failure(
                    PasswordChangeError.ValidationFailure(error)
                )
            }

        val currentUser = when (val result = userRepository.getUserById(
            authenticatedUserId
        )) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> return dataFailure(
                result.error
            )
        }

        if (!currentUser.isActive) {
            return dataFailure(
                DataError.Forbidden(
                    message = "Password change failed. " + "The authenticated account is inactive."
                )
            )
        }

        val credential = when (val result = credentialRepository.getCredentialByUserId(
                authenticatedUserId
            )) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> return dataFailure(
                result.error
            )
        }

        if (credential.passwordAlgorithm != passwordHasher.algorithm) {
            return dataFailure(
                DataError.UnknownError(
                    cause = "Credential uses an unsupported " + "password-hash algorithm."
                )
            )
        }

        val attemptedAt = Instant.now(clock)

        if (credential.isLockedAt(
                attemptedAt
            )
        ) {
            return failure(
                PasswordChangeError.TemporarilyLocked
            )
        }

        val currentPasswordCopy = currentPassword.copyOf()

        val newPasswordCopy = newPassword.copyOf()

        try {
            val currentPasswordMatches = passwordHasher.verify(
                password = currentPasswordCopy,
                encodedHash = credential.passwordHash,
            )

            if (!currentPasswordMatches) {
                val failedCredential = AuthenticationAttemptPolicy.afterFailedAttempt(
                        credential = credential,
                        attemptedAt = attemptedAt,
                    )

                when (val updateResult = credentialRepository.updateCredential(
                        failedCredential
                    )) {
                    is ResultType.Success -> Unit

                    is ResultType.Failure -> return dataFailure(
                        updateResult.error
                    )
                }

                return failure(
                    PasswordChangeError.InvalidCurrentPassword
                )
            }

            /*
             * The current password has already been verified, so direct
             * comparison safely prevents replacing it with the same value.
             */
            if (currentPasswordCopy.contentEquals(
                    newPasswordCopy
                )
            ) {
                return failure(
                    PasswordChangeError.NewPasswordMatchesCurrent
                )
            }

            val newPasswordHash = passwordHasher.hash(
                newPasswordCopy
            )

            val updatedCredential = credential.copy(
                passwordHash = newPasswordHash,
                passwordAlgorithm = passwordHasher.algorithm,
                passwordUpdatedAt = attemptedAt,
                failedLoginAttempts = 0,
                lockedUntil = null,
            )

            return when (val updateResult = credentialRepository.updateCredential(
                    updatedCredential
                )) {
                is ResultType.Success -> ResultType.Success(Unit)

                is ResultType.Failure -> dataFailure(
                    updateResult.error
                )
            }
        } catch (_: Exception) {
            return dataFailure(
                DataError.UnknownError(
                    cause = "Password-change cryptographic operation failed."
                )
            )
        } finally {
            currentPasswordCopy.fill('\u0000')
            newPasswordCopy.fill('\u0000')
        }
    }

    private fun failure(
        error: PasswordChangeError,
    ): ResultType<Unit, PasswordChangeError> = ResultType.Failure(error)

    private fun dataFailure(
        error: DataError,
    ): ResultType<Unit, PasswordChangeError> = failure(
        PasswordChangeError.DataFailure(error)
    )
}
