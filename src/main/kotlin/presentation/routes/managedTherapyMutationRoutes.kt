package com.simbiri.presentation.routes

import com.simbiri.application.therapy.session.CreateTherapyDraftUseCase
import com.simbiri.application.therapy.session.DeleteTherapyDraftUseCase
import com.simbiri.application.therapy.session.UpdateTherapyDraftUseCase
import com.simbiri.domain.model.therapy.TherapyContentStatus
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.auth.authenticatedUserIdOrRespondUnauthorized
import com.simbiri.presentation.routes.dto.therapy.management.CreatedTherapyDraftResponseDto
import com.simbiri.presentation.routes.dto.therapy.management.TherapyDraftMetadataRequestDto
import com.simbiri.presentation.routes.dto.therapy.management.toNewTherapyDraftOrFailure
import com.simbiri.presentation.routes.dto.therapy.management.toTherapyDraftUpdateOrFailure
import com.simbiri.presentation.routes.path.ManagedTherapyRoutesPath
import com.simbiri.presentation.utils.respondWithDataError
import com.simbiri.presentation.utils.therapy.parseTherapySessionIdOrFailure
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing

/**
 * Authenticated therapy-content draft mutations.
 *
 * Actor identity is always derived from the JWT principal.
 */
fun Routing.managedTherapyMutationRoutes(
    createTherapyDraftUseCase: CreateTherapyDraftUseCase,

    updateTherapyDraftUseCase: UpdateTherapyDraftUseCase,

    deleteTherapyDraftUseCase: DeleteTherapyDraftUseCase,
) {

    // POST /management/therapy-sessions
    post<ManagedTherapyRoutesPath> {
        call.preventTherapyMutationCaching()

        val actorId = authenticatedUserIdOrRespondUnauthorized() ?: return@post
        val request = call.receive<TherapyDraftMetadataRequestDto>()

        val draft = when (val result = request.toNewTherapyDraftOrFailure(
                authenticatedUserId = actorId
            )) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> {
                respondWithDataError(
                    result.error
                )

                return@post
            }
        }

        when (val result = createTherapyDraftUseCase(
            actorId = actorId,

            session = draft,
        )) {
            is ResultType.Success -> {
                val therapySessionId = result.data

                call.response.header(
                    name = HttpHeaders.Location,
                    value = "/management/therapy-sessions/" + therapySessionId.value,
                )

                call.respond(
                    status = HttpStatusCode.Created,
                    message = CreatedTherapyDraftResponseDto(
                        therapySessionId = therapySessionId.value.toString(),
                        status = TherapyContentStatus.DRAFT.name,
                        version = 1,
                    ),
                )
            }

            is ResultType.Failure -> {
                respondWithDataError(
                    result.error
                )
            }
        }
    }

    // PUT /management/therapy-sessions/{therapySessionId}
    put<ManagedTherapyRoutesPath.ById> { path ->
        call.preventTherapyMutationCaching()

        val actorId = authenticatedUserIdOrRespondUnauthorized() ?: return@put
        val therapySessionId = when (val result = parseTherapySessionIdOrFailure(
            operation = "updateTherapyDraft",

            rawTherapySessionId = path.therapySessionId,
        )) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> {
                respondWithDataError(
                    result.error
                )

                return@put
            }
        }

        val request = call.receive<TherapyDraftMetadataRequestDto>()

        val updateCandidate = when (val result = request.toTherapyDraftUpdateOrFailure(
                authenticatedUserId = actorId,
                therapySessionId = therapySessionId,
            )) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> {
                respondWithDataError(
                    result.error
                )

                return@put
            }
        }

        when (val result = updateTherapyDraftUseCase(
            actorId = actorId,
            session = updateCandidate,
        )) {
            is ResultType.Success -> {
                call.respond(
                    HttpStatusCode.NoContent
                )
            }

            is ResultType.Failure -> {
                respondWithDataError(
                    result.error
                )
            }
        }
    }

    // DELETE /management/therapy-sessions/{therapySessionId}
    delete<ManagedTherapyRoutesPath.ById> { path ->
        call.preventTherapyMutationCaching()

        val actorId = authenticatedUserIdOrRespondUnauthorized() ?: return@delete

        val therapySessionId = when (val result = parseTherapySessionIdOrFailure(
            operation = "deleteTherapyDraft",
            rawTherapySessionId = path.therapySessionId,
        )) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> {
                respondWithDataError(
                    result.error
                )

                return@delete
            }
        }

        when (val result = deleteTherapyDraftUseCase(
            actorId = actorId,
            therapySessionId = therapySessionId,
        )) {
            is ResultType.Success -> {
                call.respond(
                    HttpStatusCode.NoContent
                )
            }

            is ResultType.Failure -> {
                respondWithDataError(
                    result.error
                )
            }
        }
    }
}

/**
 * Prevents management responses from being cached by clients or proxies.
 */
private fun ApplicationCall.preventTherapyMutationCaching() {
    response.header(
        name = HttpHeaders.CacheControl,
        value = "no-store",
    )

    response.header(
        name = HttpHeaders.Pragma,
        value = "no-cache",
    )
}
