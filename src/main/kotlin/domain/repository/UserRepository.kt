package com.simbiri.domain.repository

import com.simbiri.domain.model.user.User
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

interface UserRepository {
    suspend fun getAllUsers(userType: Int?): ResultType<List<User>, DataError>
    suspend fun getUserById(userId: String?): ResultType<User, DataError>
    suspend fun upsertUser(userRec: User): ResultType<Unit, DataError>
    suspend fun deleteUserById(userId: String?): ResultType<Unit, DataError>
    suspend fun insertUsersInBulk(users: List<User>): ResultType<Unit, DataError>
}