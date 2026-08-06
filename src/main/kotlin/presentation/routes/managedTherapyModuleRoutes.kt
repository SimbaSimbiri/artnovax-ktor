package com.simbiri.presentation.routes

import com.simbiri.application.therapy.module.AddTherapyModuleUseCase
import com.simbiri.application.therapy.module.RemoveTherapyModuleUseCase
import com.simbiri.application.therapy.module.ReorderTherapyModulesUseCase
import com.simbiri.application.therapy.module.UpdateTherapyModuleUseCase
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.auth.authenticatedUserIdOrRespondUnauthorized
import com.simbiri.presentation.routes.dto.therapy.management.CreateTherapyModuleRequestDto
import com.simbiri.presentation.routes.dto.therapy.management.CreatedTherapyModuleResponseDto
import com.simbiri.presentation.routes.dto.therapy.management.ReorderTherapyModulesRequestDto
import com.simbiri.presentation.routes.dto.therapy.management.UpdateTherapyModuleRequestDto
import com.simbiri.presentation.routes.dto.therapy.management.toTherapyModuleIdsOrFailure
import com.simbiri.presentation.routes.dto.therapy.management.toTherapyModuleOrFailure
import com.simbiri.presentation.routes.path.ManagedTherapyRoutesPath
import com.simbiri.presentation.utils.respondWithDataError
import com.simbiri.presentation.utils.therapy.parseTherapyModuleIdOrFailure
import com.simbiri.presentation.utils.therapy.parseTherapySessionIdOrFailure
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing

fun Routing.managedTherapyModuleRoutes(
    addTherapyModuleUseCase: AddTherapyModuleUseCase,
    updateTherapyModuleUseCase: UpdateTherapyModuleUseCase,
    reorderTherapyModulesUseCase: ReorderTherapyModulesUseCase,
    removeTherapyModuleUseCase: RemoveTherapyModuleUseCase,
) {

    // POST /management/therapy-sessions/{therapySessionId}/modules
    post<ManagedTherapyRoutesPath.Modules> { path ->
        call.preventTherapyMutationCaching()

        val actorId = authenticatedUserIdOrRespondUnauthorized() ?: return@post

        val therapySessionId = when (val result = parseTherapySessionIdOrFailure(
            operation = "addTherapyModule",
            rawTherapySessionId = path.therapySessionId,
        )) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> {
                respondWithDataError(result.error)
                return@post
            }
        }

        val request = call.receive<CreateTherapyModuleRequestDto>()

        val module = when (val result = request.toTherapyModuleOrFailure()) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> {
                respondWithDataError(result.error)
                return@post
            }
        }

        when (val result = addTherapyModuleUseCase(
            actorId = actorId,
            therapySessionId = therapySessionId,
            module = module,
        )) {
            is ResultType.Success -> {
                val therapyModuleId = result.data

                call.response.header(
                    name = HttpHeaders.Location,
                    value = "/management/therapy-sessions/${therapySessionId.value}/modules/${therapyModuleId.value}",
                )

                call.respond(
                    status = HttpStatusCode.Created,
                    message = CreatedTherapyModuleResponseDto(
                        therapyModuleId = therapyModuleId.value.toString(),
                    ),
                )
            }

            is ResultType.Failure -> respondWithDataError(result.error)
        }
    }

    // PUT /management/therapy-sessions/{therapySessionId}/modules/{therapyModuleId}
    put<ManagedTherapyRoutesPath.Modules.ById> { path ->
        call.preventTherapyMutationCaching()

        val actorId = authenticatedUserIdOrRespondUnauthorized() ?: return@put

        val therapySessionId = when (val result = parseTherapySessionIdOrFailure(
            operation = "updateTherapyModule",
            rawTherapySessionId = path.parent.therapySessionId,
        )) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> {
                respondWithDataError(result.error)
                return@put
            }
        }

        val therapyModuleId = when (val result = parseTherapyModuleIdOrFailure(
            operation = "updateTherapyModule",
            field = "therapyModuleId",
            rawTherapyModuleId = path.therapyModuleId,
        )) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> {
                respondWithDataError(result.error)
                return@put
            }
        }

        val request = call.receive<UpdateTherapyModuleRequestDto>()

        val module = when (val result = request.toTherapyModuleOrFailure(therapyModuleId)) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> {
                respondWithDataError(result.error)
                return@put
            }
        }

        when (val result = updateTherapyModuleUseCase(
            actorId = actorId,
            therapySessionId = therapySessionId,
            module = module,
        )) {
            is ResultType.Success -> call.respond(HttpStatusCode.NoContent)
            is ResultType.Failure -> respondWithDataError(result.error)
        }
    }

    // PUT /management/therapy-sessions/{therapySessionId}/modules/reorder
    put<ManagedTherapyRoutesPath.Modules.Reorder> { path ->
        call.preventTherapyMutationCaching()

        val actorId = authenticatedUserIdOrRespondUnauthorized() ?: return@put

        val therapySessionId = when (val result = parseTherapySessionIdOrFailure(
            operation = "reorderTherapyModules",
            rawTherapySessionId = path.parent.therapySessionId,
        )) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> {
                respondWithDataError(result.error)
                return@put
            }
        }

        val request = call.receive<ReorderTherapyModulesRequestDto>()

        val orderedModuleIds = when (val result = request.toTherapyModuleIdsOrFailure()) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> {
                respondWithDataError(result.error)
                return@put
            }
        }

        when (val result = reorderTherapyModulesUseCase(
            actorId = actorId,
            therapySessionId = therapySessionId,
            orderedModuleIds = orderedModuleIds,
        )) {
            is ResultType.Success -> call.respond(HttpStatusCode.NoContent)
            is ResultType.Failure -> respondWithDataError(result.error)
        }
    }

    // DELETE /management/therapy-sessions/{therapySessionId}/modules/{therapyModuleId}
    delete<ManagedTherapyRoutesPath.Modules.ById> { path ->
        call.preventTherapyMutationCaching()

        val actorId = authenticatedUserIdOrRespondUnauthorized() ?: return@delete

        val therapySessionId = when (val result = parseTherapySessionIdOrFailure(
            operation = "removeTherapyModule",
            rawTherapySessionId = path.parent.therapySessionId,
        )) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> {
                respondWithDataError(result.error)
                return@delete
            }
        }

        val therapyModuleId = when (val result = parseTherapyModuleIdOrFailure(
            operation = "removeTherapyModule",
            field = "therapyModuleId",
            rawTherapyModuleId = path.therapyModuleId,
        )) {
            is ResultType.Success -> result.data
            is ResultType.Failure -> {
                respondWithDataError(result.error)
                return@delete
            }
        }

        when (val result = removeTherapyModuleUseCase(
            actorId = actorId,
            therapySessionId = therapySessionId,
            therapyModuleId = therapyModuleId,
        )) {
            is ResultType.Success -> call.respond(HttpStatusCode.NoContent)
            is ResultType.Failure -> respondWithDataError(result.error)
        }
    }
}
