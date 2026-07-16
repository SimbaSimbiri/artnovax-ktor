package com.simbiri.domain.repository

import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.user.User
import com.simbiri.domain.model.user.UserType
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

interface UserRepository {

    // -----------------------------------------------------------------
    // Target repository API
    // -----------------------------------------------------------------

    /**
     * Retrieves all users, optionally filtered by a valid domain user type.
     */
    suspend fun getUsers(
        userType: UserType? = null,
    ): ResultType<List<User>, DataError> =
        getAllUsers(
            userType = userType?.code,
        )

    /**
     * Retrieves a user using a typed domain identifier.
     */
    suspend fun getUserById(
        userId: UserId,
    ): ResultType<User, DataError> =
        getUserById(
            userId = userId.value.toString(),
        )

    /**
     * Persists a new user.
     *
     * The application layer guarantees that user.id is null.
     */
    suspend fun createUser(
        user: User,
    ): ResultType<Unit, DataError> =
        upsertUser(user)

    /**
     * Persists changes to an existing user.
     *
     * The application layer guarantees that user.id is non-null.
     */
    suspend fun updateUser(
        user: User,
    ): ResultType<Unit, DataError> =
        upsertUser(user)

    /**
     * Creates multiple users in a single transactional operation.
     */
    suspend fun createUsers(
        users: List<User>,
    ): ResultType<Unit, DataError> =
        insertUsersInBulk(users)

    /**
     * Deletes a user using a typed domain identifier.
     */
    suspend fun deleteUserById(
        userId: UserId,
    ): ResultType<Unit, DataError> =
        deleteUserById(
            userId = userId.value.toString(),
        )

    // -----------------------------------------------------------------
    // Legacy repository API
    //
    // These methods remain temporarily so UserRepoImpl and the existing
    // routes continue compiling while the user pipeline is migrated.
    // They will be removed after the implementation and callers use the
    // typed API above.
    // -----------------------------------------------------------------

    @Deprecated(
        message = "Use getUsers(UserType?) instead.",
    )
    suspend fun getAllUsers(
        userType: Int?,
    ): ResultType<List<User>, DataError>

    @Deprecated(
        message = "Use getUserById(UserId) instead.",
    )
    suspend fun getUserById(
        userId: String?,
    ): ResultType<User, DataError>

    @Deprecated(
        message = "Use createUser(User) or updateUser(User) instead.",
    )
    suspend fun upsertUser(
        userRec: User,
    ): ResultType<Unit, DataError>

    @Deprecated(
        message = "Use createUsers(List<User>) instead.",
    )
    suspend fun insertUsersInBulk(
        users: List<User>,
    ): ResultType<Unit, DataError>

    @Deprecated(
        message = "Use deleteUserById(UserId) instead.",
    )
    suspend fun deleteUserById(
        userId: String?,
    ): ResultType<Unit, DataError>
}