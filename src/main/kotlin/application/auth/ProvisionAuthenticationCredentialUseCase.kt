package com.simbiri.application.auth

import com.simbiri.domain.model.auth.AuthenticationCredential
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.policy.auth.PasswordPolicy
import com.simbiri.domain.repository.AuthenticationCredentialRepository
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.security.PasswordHasher
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.time.Clock
import java.time.Instant

/**
 * Creates the first password credential for an existing persisted user.
 *
 * This operation does not replace an existing credential. Password changes
 * will use a separate use case so creation and replacement remain explicit.
 */
class ProvisionAuthenticationCredentialUseCase(
    private val userRepository: UserRepository,
    private val credentialRepository: AuthenticationCredentialRepository,
    private val passwordHasher: PasswordHasher,
    private val clock: Clock,
) {

    suspend operator fun invoke(
        userId: UserId,
        password: CharArray,
    ): ResultType<Unit, DataError> {
        PasswordPolicy.validateForCredentialCreation(
                password
            )?.let { error ->
                return ResultType.Failure(error)
            }

        /*
         * Verify the target user before performing
         * expensive password-hashing.
         */
        when (val userResult = userRepository.getUserById(userId)) {
            is ResultType.Success -> Unit

            is ResultType.Failure -> return ResultType.Failure(
                userResult.error
            )
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
            /*
             * The hasher clears its own internal copy. The application
             * layer also clears the copy it created for this operation.
            */
            workingPassword.fill('\u0000')
        }

        val now = Instant.now(clock)

        val credential = AuthenticationCredential(
            userId = userId,
            passwordHash = encodedPasswordHash,
            passwordAlgorithm = passwordHasher.algorithm,
            passwordUpdatedAt = now,
            failedLoginAttempts = 0,
            lockedUntil = null,
            sessionVersion = 1L,
            createdAt = null,
            updatedAt = null,
        )

        return credentialRepository.createCredential(credential)
    }
}
