package com.simbiri.presentation.routes

import com.simbiri.application.user.*
import com.simbiri.domain.util.ResultType
import com.simbiri.domain.util.onFailure
import com.simbiri.domain.util.onSuccess
import com.simbiri.presentation.routes.dto.user.UserUpsertDto
import com.simbiri.presentation.routes.dto.user.toDomainForCreate
import com.simbiri.presentation.routes.dto.user.toDomainForUpdate
import com.simbiri.presentation.routes.dto.user.toResponseDto
import com.simbiri.presentation.routes.path.UserRoutesPath
import com.simbiri.presentation.utils.parseUserIdOrFailure
import com.simbiri.presentation.utils.parseUserTypeFilterOrFailure
import com.simbiri.presentation.utils.respondWithDataError
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.*
import io.ktor.server.routing.Routing

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
            val parsed = parseUserTypeFilterOrFailure(
                operation = "getUsers",
                rawUserType = path.userType,
            )
        ) {
            is ResultType.Success -> parsed.data

            is ResultType.Failure -> {
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
        val userId = when (
            val parsed = parseUserIdOrFailure(
                operation = "getUserById",
                rawUserId = path.userId,
            )
        ) {
            is ResultType.Success -> parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@get
            }
        }

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
                    status = HttpStatusCode.Created,
                    message = "${users.size} users added successfully",
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // PUT /users/{userId}
    put<UserRoutesPath.ById> { path ->
        val userId = when (
            val parsed = parseUserIdOrFailure(
                operation = "updateUser",
                rawUserId = path.userId,
            )
        ) {
            is ResultType.Success -> parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@put
            }
        }

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
        val userId = when (
            val parsed = parseUserIdOrFailure(
                operation = "deleteUserById",
                rawUserId = path.userId,
            )
        ) {
            is ResultType.Success -> parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@delete
            }
        }

        deleteUserUseCase(userId)
            .onSuccess {
                call.respond(
                    HttpStatusCode.NoContent
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }
}