package com.simbiri.application.auth

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.repository.AccessTokenSessionCommandRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Invalidates every access token issued for the authenticated user.
 *
 * This includes the token used to invoke the operation.
 */
class LogoutAllDevicesUseCase(
    private val accessTokenSessionCommandRepository: AccessTokenSessionCommandRepository,
) {

    suspend operator fun invoke(
        authenticatedUserId: UserId,
    ): ResultType<Unit, DataError> = when (val result = accessTokenSessionCommandRepository.invalidateAllSessions(
        authenticatedUserId
    )) {
        is ResultType.Success -> result

        is ResultType.Failure -> {/*
            * Valid JWT should always have a corresponding credential.
            * Its disappearance indicates inconsistent server state,
            * and not a client-addressable missing resource.
            */
            if (result.error == DataError.NotFound) {
                ResultType.Failure(
                    DataError.UnknownError(
                        cause = "Authenticated credential could not be found."
                    )
                )
            } else {
                result
            }
        }
    }
}
