package com.simbiri.application.auth

import com.simbiri.domain.model.auth.AuthenticatedSession
import com.simbiri.domain.model.auth.AuthenticationCredential
import com.simbiri.domain.model.auth.AuthenticationError
import com.simbiri.domain.model.auth.RefreshSession
import com.simbiri.domain.model.common.RefreshTokenFamilyId
import com.simbiri.domain.policy.auth.AuthenticationAttemptPolicy
import com.simbiri.domain.policy.auth.PasswordPolicy
import com.simbiri.domain.policy.user.EmailAddressNormalizer
import com.simbiri.domain.repository.AuthenticationCredentialRepository
import com.simbiri.domain.repository.RefreshSessionRepository
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.security.AccessTokenIssuer
import com.simbiri.domain.security.PasswordHasher
import com.simbiri.domain.security.RefreshTokenIssuer
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Authenticates one active user using an email address and password.
 *
 * Unknown users, inactive users, missing credentials, incorrect passwords,
 * and temporarily locked credentials are not distinguished at the HTTP
 * boundary.
 *
 * Successful authentication creates:
 * - one short-lived access token;
 * - one opaque refresh token;
 * - one persisted refresh-session record containing only the refresh-token
 *   hash.
 */
class AuthenticateUserUseCase(
    private val userRepository: UserRepository,
    private val credentialRepository: AuthenticationCredentialRepository,
    private val refreshSessionRepository: RefreshSessionRepository,
    private val passwordHasher: PasswordHasher,
    private val accessTokenIssuer: AccessTokenIssuer,
    private val refreshTokenIssuer: RefreshTokenIssuer,
    private val clock: Clock,
) {

    suspend operator fun invoke(
        emailAddress: String,
        password: CharArray,
    ): ResultType< AuthenticatedSession, AuthenticationError,> {
        val normalizedEmailAddress = EmailAddressNormalizer.normalize(
            emailAddress
        )

        /*
         * Reject malformed authentication material before repository access
         * or expensive password verification.
         *
         * The same InvalidCredentials result is returned for malformed
         * material so the endpoint does not disclose authentication state.
         */
        if (normalizedEmailAddress.isBlank() || normalizedEmailAddress.length > MAXIMUM_EMAIL_ADDRESS_LENGTH ||
            PasswordPolicy.validateForCredentialCreation(
                    password
                ) != null
        ) {
            return invalidCredentials()
        }

        val user = when (val result = userRepository.getUserByEmailAddress(
                normalizedEmailAddress
            )) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> return mapUserLookupFailure(
                result.error
            )
        }

        val userId = user.id ?: return dataFailure(
            DataError.UnknownError(
                cause = "Persisted authentication user " + "is missing its ID."
            )
        )

        if (!user.isActive) {
            return invalidCredentials()
        }

        val credential = when (val result = credentialRepository.getCredentialByUserId( userId)) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> return mapCredentialLookupFailure(
                result.error
            )
        }

        val attemptedAt = Instant.now(clock)

        if (credential.isLockedAt(attemptedAt)
        ) {
            return ResultType.Failure(
                AuthenticationError.TemporarilyLocked
            )
        }

        if (credential.passwordAlgorithm != passwordHasher.algorithm) {
            return dataFailure(
                DataError.UnknownError(
                    cause = "Credential uses an unsupported " + "password-hash algorithm."
                )
            )
        }

        val passwordMatches = verifyPasswordOrFailure(
            password = password,
            credential = credential,
        ) ?: return dataFailure(
            DataError.UnknownError(
                cause = "Password verification failed."
            )
        )

        if (!passwordMatches) {
            val failedCredential = AuthenticationAttemptPolicy.afterFailedAttempt(
                    credential = credential,
                    attemptedAt = attemptedAt,
                )

            when (val result = credentialRepository.updateCredential(
                    failedCredential
                )) {
                is ResultType.Success -> Unit

                is ResultType.Failure -> return dataFailure(
                    result.error
                )
            }

            return invalidCredentials()
        }

        val successfulCredential = AuthenticationAttemptPolicy.afterSuccessfulAttempt(
                credential
            )

        if (successfulCredential != credential) {
            when (val result = credentialRepository.updateCredential(
                    successfulCredential
                )) {
                is ResultType.Success -> Unit

                is ResultType.Failure -> return dataFailure(
                    result.error
                )
            }
        }

        /*
         * Create the access token first. It is not returned unless refresh
         * token issuance and refresh-session persistence also succeed.
         */
        val issuedAccessToken = try {
            accessTokenIssuer.issue(
                userId = userId,
                sessionVersion = successfulCredential.sessionVersion,
            )
        } catch (_: Exception) {
            return dataFailure(
                DataError.UnknownError(
                    cause = "Access-token issuance failed."
                )
            )
        }

        /*
         * The issuer returns both the plaintext refresh token and its
         * SHA-256 hash. Only the hash is persisted.
         */
        val issuedRefreshToken = try {
            refreshTokenIssuer.issue()
        } catch (_: Exception) {
            return dataFailure(
                DataError.UnknownError(
                    cause = "Refresh-token issuance failed."
                )
            )
        }

        val refreshSession = RefreshSession(
            familyId = RefreshTokenFamilyId(
                UUID.randomUUID()
            ),
            userId = userId,
            tokenHash = issuedRefreshToken.hash,
            sessionVersion = successfulCredential.sessionVersion,
            expiresAt = issuedRefreshToken.expiresAt,
        )

        when (val result = refreshSessionRepository.createSession(
                refreshSession
            )) {
            is ResultType.Success -> Unit

            is ResultType.Failure -> return dataFailure(
                result.error
            )
        }

        /*
         * The plaintext refresh token is returned only after its hash has
         * been persisted successfully.
         */
        return ResultType.Success(
            AuthenticatedSession(
                userId = userId,

                accessToken = issuedAccessToken.value,

                tokenType = "Bearer",

                accessTokenExpiresAt = issuedAccessToken.expiresAt,

                refreshToken = issuedRefreshToken.value,

                refreshTokenExpiresAt = issuedRefreshToken.expiresAt,
            )
        )
    }

    /**
     * Verifies an application-owned password copy so this use case can clear
     * its plaintext working memory after the password hasher returns.
     *
     * Null means the password-verification infrastructure failed.
     */
    private suspend fun verifyPasswordOrFailure(
        password: CharArray,
        credential: AuthenticationCredential,
    ): Boolean? {
        val workingPassword = password.copyOf()

        return try {
            passwordHasher.verify(
                password = workingPassword,

                encodedHash = credential.passwordHash,
            )
        } catch (_: Exception) {
            null
        } finally {
            workingPassword.fill(
                '\u0000'
            )
        }
    }

    /**
     * Unknown users become generic invalid-credential failures.
     *
     * Infrastructure failures remain server-side data failures.
     */
    private fun mapUserLookupFailure(
        error: DataError,
    ): ResultType<
            AuthenticatedSession,
            AuthenticationError,
            > = if (error == DataError.NotFound) {
        invalidCredentials()
    } else {
        dataFailure(error)
    }

    /**
     * Missing credentials become generic invalid-credential failures.
     *
     * Infrastructure failures remain server-side data failures.
     */
    private fun mapCredentialLookupFailure(
        error: DataError,
    ): ResultType<
            AuthenticatedSession,
            AuthenticationError,
            > = if (error == DataError.NotFound) {
        invalidCredentials()
    } else {
        dataFailure(error)
    }

    private fun invalidCredentials(): ResultType<
            AuthenticatedSession,
            AuthenticationError,
            > = ResultType.Failure(
        AuthenticationError.InvalidCredentials
    )

    private fun dataFailure(
        error: DataError,
    ): ResultType<
            AuthenticatedSession,
            AuthenticationError,
            > = ResultType.Failure(
        AuthenticationError.DataFailure(error)
    )

    private companion object {
        const val MAXIMUM_EMAIL_ADDRESS_LENGTH = 255
    }
}
