package com.simbiri.domain.repository

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.user.User
import com.simbiri.domain.model.user.UserType
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Defines persistence operations available for the User aggregate.
 *
 * The repository now accepts domain types only:
 * - UserId instead of raw UUID strings;
 * - UserType instead of integer type codes;
 * - explicit create and update operations instead of an inferred upsert.
 *
 * HTTP parsing, request validation, and DTO mapping belong to the
 * presentation and application layers.
 */
interface UserRepository {

    /**
     * Retrieves all users, optionally filtered by user type.
     */
    suspend fun getUsers(
        userType: UserType? = null,
    ): ResultType<List<User>, DataError>

    /**
     * Retrieves one user by its typed domain identifier.
     */
    suspend fun getUserById(
        userId: UserId,
    ): ResultType<User, DataError>

    /**
     * Retrieves one user using a normalized email address.
     *
     * performs defensive normalization even when the caller
     * has already normalized the value.
     */
    suspend fun getUserByEmailAddress(
        emailAddress: String,
    ): ResultType<User, DataError>

    /**
     * Persists a new user.
     *
     * The application layer must provide a user whose ID is null.
     */
    suspend fun createUser(
        user: User,
    ): ResultType<Unit, DataError>

    /**
     * Persists changes to an existing user.
     *
     * The application layer must provide a user whose ID is non-null.
     */
    suspend fun updateUser(
        user: User,
    ): ResultType<Unit, DataError>

    /**
     * Creates multiple users in one transactional operation.
     */
    suspend fun createUsers(
        users: List<User>,
    ): ResultType<Unit, DataError>

    /**
     * Deletes one user using its typed domain identifier.
     */
    suspend fun deleteUserById(
        userId: UserId,
    ): ResultType<Unit, DataError>
}