package com.simbiri.presentation.config

import com.simbiri.application.user.CreateUserUseCase
import com.simbiri.application.user.CreateUsersInBulkUseCase
import com.simbiri.application.user.DeleteUserUseCase
import com.simbiri.application.user.GetUserByIdUseCase
import com.simbiri.application.user.GetUsersUseCase
import com.simbiri.application.user.UpdateUserUseCase
import com.simbiri.domain.repository.CommunityRepository
import com.simbiri.presentation.routes.communityRoutes
import com.simbiri.presentation.routes.root
import com.simbiri.presentation.routes.userRoutes
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.resources.Resources
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    install(Resources)

    // User application use cases
    val createUserUseCase: CreateUserUseCase by inject()
    val createUsersInBulkUseCase: CreateUsersInBulkUseCase by inject()
    val getUserByIdUseCase: GetUserByIdUseCase by inject()
    val getUsersUseCase: GetUsersUseCase by inject()
    val updateUserUseCase: UpdateUserUseCase by inject()
    val deleteUserUseCase: DeleteUserUseCase by inject()

    // Community pipeline has not been migrated yet.
    val communityRepository: CommunityRepository by inject()

    routing {
        root()

        userRoutes(
            createUserUseCase = createUserUseCase,
            createUsersInBulkUseCase = createUsersInBulkUseCase,
            getUserByIdUseCase = getUserByIdUseCase,
            getUsersUseCase = getUsersUseCase,
            updateUserUseCase = updateUserUseCase,
            deleteUserUseCase = deleteUserUseCase,
        )

        communityRoutes(communityRepository)
    }
}