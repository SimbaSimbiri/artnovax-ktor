package com.simbiri.presentation.routes

import com.simbiri.application.user.CreateUserUseCase
import com.simbiri.application.user.CreateUsersInBulkUseCase
import com.simbiri.application.user.DeleteUserUseCase
import com.simbiri.application.user.GetUserByIdUseCase
import com.simbiri.application.user.GetUsersUseCase
import com.simbiri.application.user.UpdateUserUseCase
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.user.UserType
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.onFailure
import com.simbiri.domain.util.onSuccess
import com.simbiri.presentation.routes.dto.user.UserUpsertDto
import com.simbiri.presentation.routes.dto.user.toDomainForCreate
import com.simbiri.presentation.routes.dto.user.toDomainForUpdate
import com.simbiri.presentation.routes.dto.user.toResponseDto
import com.simbiri.presentation.routes.path.UserRoutesPath
import com.simbiri.presentation.utils.respondWithDataError
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingContext
import java.util.UUID

fun Routing.userRoutes(
    createUserUseCase: CreateUserUseCase,
    createUsersInBulkUseCase: CreateUsersInBulkUseCase,
    getUserByIdUseCase: GetUserByIdUseCase,
    getUsersUseCase: GetUsersUseCase,
    updateUserUseCase: UpdateUserUseCase,
    deleteUserUseCase: DeleteUserUseCase,
) {

    // GET /users?userType={code}
    get<UserRoutesPath> { path ->
        val userType = when (
            val parsed = parseUserTypeFilter(
                rawUserType = path.userType,
            )
        ) {
            is UserTypeFilterResult.Valid -> parsed.userType

            is UserTypeFilterResult.Invalid -> {
                respondWithDataError(parsed.error)
                return@get
            }
        }

        getUsersUseCase(userType)
            .onSuccess { users ->
                call.respond(
                    status = HttpStatusCode.OK,
                    message = users.toResponseDto(),
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // GET /users/{userId}
    get<UserRoutesPath.ById> { path ->
        val userId = parseUserIdOrRespond(
            operation = "getUserById",
            rawUserId = path.userId,
        ) ?: return@get

        getUserByIdUseCase(userId)
            .onSuccess { user ->
                call.respond(
                    status = HttpStatusCode.OK,
                    message = user.toResponseDto(),
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // POST /users
    post<UserRoutesPath> {
        val userReceivedDto = call.receive<UserUpsertDto>()
        val user = userReceivedDto.toDomainForCreate()

        createUserUseCase(user)
            .onSuccess {
                call.respond(
                    status = HttpStatusCode.Created,
                    message = "User ${user.accountName} created successfully",
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // POST /users/bulk
    post<UserRoutesPath.Bulk> {
        val usersReceivedDto = call.receive<List<UserUpsertDto>>()
        val users = usersReceivedDto.toDomainForCreate()

        createUsersInBulkUseCase(users)
            .onSuccess {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = "${users.size} users added successfully",
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // PUT /users/{userId}
    put<UserRoutesPath.ById> { path ->
        val userId = parseUserIdOrRespond(
            operation = "updateUser",
            rawUserId = path.userId,
        ) ?: return@put

        val userReceivedDto = call.receive<UserUpsertDto>()
        val user = userReceivedDto.toDomainForUpdate(
            userId = userId.value,
        )

        updateUserUseCase(user)
            .onSuccess {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = "User ${user.accountName} updated successfully",
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // DELETE /users/{userId}
    delete<UserRoutesPath.ById> { path ->
        val userId = parseUserIdOrRespond(
            operation = "deleteUserById",
            rawUserId = path.userId,
        ) ?: return@delete

        deleteUserUseCase(userId)
            .onSuccess {
                call.respond(
                    HttpStatusCode.NoContent,
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }
}

/**
 * Parses the optional user-type query parameter.
 *
 * A null query parameter is valid and means that users should not
 * be filtered by type.
 */
private fun parseUserTypeFilter(
    rawUserType: String?,
): UserTypeFilterResult {
    if (rawUserType == null) {
        return UserTypeFilterResult.Valid(
            userType = null,
        )
    }

    if (rawUserType.isBlank()) {
        return UserTypeFilterResult.Invalid(
            error = DataError.ValidationError(
                message = "User retrieval failed. Query parameter " +
                        "'userType' cannot be blank when provided."
            )
        )
    }

    val userTypeCode = rawUserType.toIntOrNull()
        ?: return UserTypeFilterResult.Invalid(
            error = DataError.ValidationError(
                message = "User retrieval failed. Query parameter " +
                        "'userType' must be a valid integer. " +
                        "receivedValue='$rawUserType'."
            )
        )

    val userType = UserType.fromCodeOrNull(userTypeCode)
        ?: return UserTypeFilterResult.Invalid(
            error = DataError.ValidationError(
                message = "User retrieval failed. Query parameter " +
                        "'userType' contains an unsupported user-type code. " +
                        "receivedValue='$rawUserType', " +
                        "parsedCode=$userTypeCode, " +
                        "supportedCodes=${UserType.entries.map { it.code }}."
            )
        )

    return UserTypeFilterResult.Valid(
        userType = userType,
    )
}

/**
 * Parses an HTTP path value into the typed domain UserId.
 *
 * UUID parsing belongs to presentation because the raw string originates
 * from the HTTP request path.
 */
private suspend fun RoutingContext.parseUserIdOrRespond(
    operation: String,
    rawUserId: String,
): UserId? {
    if (rawUserId.isBlank()) {
        respondWithDataError(
            DataError.ValidationError(
                message = "User ID validation failed in $operation. " +
                        "Field 'userId' is required and cannot be blank."
            )
        )

        return null
    }

    val uuid = runCatching {
        UUID.fromString(rawUserId)
    }.getOrNull()

    if (uuid == null) {
        respondWithDataError(
            DataError.ValidationError(
                message = "User ID validation failed in $operation. " +
                        "Field 'userId' must be a valid UUID. " +
                        "receivedValue='$rawUserId'."
            )
        )

        return null
    }

    return UserId(uuid)
}

private sealed interface UserTypeFilterResult {

    data class Valid(
        val userType: UserType?,
    ) : UserTypeFilterResult

    data class Invalid(
        val error: DataError.ValidationError,
    ) : UserTypeFilterResult
}