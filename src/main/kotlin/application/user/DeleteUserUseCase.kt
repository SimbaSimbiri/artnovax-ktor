package com.simbiri.application.user

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

class DeleteUserUseCase( private val userRepository: UserRepository) {
    suspend operator fun invoke(userId: UserId): ResultType<Unit, DataError> {

        return userRepository.deleteUserById(userId = userId)
    }
}