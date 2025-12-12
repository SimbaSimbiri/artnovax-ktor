package com.simbiri.presentation.routes

import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.onFailure
import com.simbiri.domain.util.onSuccess
import com.simbiri.presentation.routes.dto.user.UserUpsertDto
import com.simbiri.presentation.routes.dto.user.toDomainForCreate
import com.simbiri.presentation.routes.dto.user.toDomainForUpdate
import com.simbiri.presentation.routes.dto.user.toResponseDto
import com.simbiri.presentation.routes.path.UserRoutesPath
import com.simbiri.presentation.utils.respondWithDataError
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Routing.userRoutes(userRepository: UserRepository) {

    // GET ALL USERS
    get<UserRoutesPath> { path ->
        userRepository.getAllUsers(path.userType?.toInt())
            .onSuccess { users ->
                call.respond(message = users.toResponseDto(), status = HttpStatusCode.OK)
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    //GET USER BY ID
    get<UserRoutesPath.ById> { path ->
        userRepository.getUserById(path.userId)
            .onSuccess { user ->
                call.respond(message = user.toResponseDto(), status = HttpStatusCode.OK)
            }
            .onFailure { error ->
                respondWithDataError(error)
            }

    }

    //POST /users
    post<UserRoutesPath> {

        val userReceivedDto = call.receive<UserUpsertDto>()
        val user = userReceivedDto.toDomainForCreate()

        userRepository.upsertUser(user)
            .onSuccess {
                call.respond(
                    message = "User ${user.accountName} created successfully",
                    status = HttpStatusCode.Created
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // POST /users/bulk add users in bulk
    post<UserRoutesPath.Bulk>{
        val usersReceivedDto = call.receive<List<UserUpsertDto>>()
        val users = usersReceivedDto.toDomainForCreate()

        userRepository.insertUsersInBulk(users)
            .onSuccess {
                call.respond(
                    message = "${users.size} users added successfully",
                    status = HttpStatusCode.OK
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }

    }

    // PUT /users/{userId}
    put<UserRoutesPath.ById> { path ->

        val userReceivedDto = call.receive<UserUpsertDto>()
        val userUuid = UUID.fromString(path.userId)
        val user = userReceivedDto.toDomainForUpdate(userId = userUuid)

        userRepository.upsertUser(user)
            .onSuccess {
                call.respond(
                    message = "User ${user.accountName} updated successfully",
                    status = HttpStatusCode.OK
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // DELETE USER
    delete<UserRoutesPath.ById> { path ->
        userRepository.deleteUserById(path.userId)
            .onSuccess {
                call.respond(
                    message = "User and SocialLinks associated with userId ${path.userId} deleted successfully",
                    status = HttpStatusCode.OK
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

}