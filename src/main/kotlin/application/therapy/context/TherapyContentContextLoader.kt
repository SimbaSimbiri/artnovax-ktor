package com.simbiri.application.therapy.context

import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.user.User
import com.simbiri.domain.repository.TherapyContentRepository
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Resolves persisted actor and TherapySession required by
 * therapy-content use cases.
 *
 * Authorization is still in TherapyContentAccessPolicy.
 */
class TherapyContentContextLoader(
    private val userRepository: UserRepository,
    private val therapyContentRepository: TherapyContentRepository,
) {

    suspend fun loadActor(
        actorId: UserId,
    ): ResultType<User, DataError> =
        userRepository.getUserById(actorId)

    suspend fun load(
        actorId: UserId,
        therapySessionId: TherapySessionId,
    ): ResultType<TherapyContentContext, DataError> {
        val actor =
            when (
                val result =
                    userRepository.getUserById(actorId)
            ) {
                is ResultType.Success ->
                    result.data

                is ResultType.Failure ->
                    return ResultType.Failure(result.error)
            }

        val session =
            when (
                val result =
                    therapyContentRepository
                        .getTherapySessionById(
                            therapySessionId
                        )
            ) {
                is ResultType.Success ->
                    result.data

                is ResultType.Failure ->
                    return ResultType.Failure(result.error)
            }

        return ResultType.Success(
            TherapyContentContext(
                actor = actor,
                session = session,
            )
        )
    }
}