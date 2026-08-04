package com.simbiri.application.auth

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.repository.AuthenticationCredentialRepository
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.ResultType

/**
 * Confirms that a cryptographically valid JWT still represents a current
 * server-side authentication session.
 *
 * All repository failures reject the token. Authentication validation must
 * fail closed when server-side state cannot be confirmed.
 */
class ValidateAccessTokenSessionUseCase(
    private val userRepository: UserRepository,
    private val credentialRepository: AuthenticationCredentialRepository,
) {

    suspend operator fun invoke(
        userId: UserId,
        sessionVersion: Long,
    ): Boolean {
        if (sessionVersion <= 0L) {
            return false
        }

        val user = when (val result = userRepository.getUserById(userId)) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> return false
        }

        if (user.id != userId || !user.isActive) {
            return false
        }

        val credential = when (val result = credentialRepository.getCredentialByUserId(
                userId
            )) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> return false
        }

        return credential.userId == userId && credential.sessionVersion == sessionVersion
    }
}
