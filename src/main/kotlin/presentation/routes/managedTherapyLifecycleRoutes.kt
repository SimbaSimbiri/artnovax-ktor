package com.simbiri.presentation.routes

import com.simbiri.application.therapy.lifecycle.ArchiveTherapyContentUseCase
import com.simbiri.application.therapy.lifecycle.PublishTherapyContentUseCase
import com.simbiri.application.therapy.lifecycle.ReturnTherapyContentToDraftUseCase
import com.simbiri.application.therapy.lifecycle.SubmitTherapyContentForReviewUseCase
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.auth.authenticatedUserIdOrRespondUnauthorized
import com.simbiri.presentation.routes.path.ManagedTherapyRoutesPath
import com.simbiri.presentation.utils.respondWithDataError
import com.simbiri.presentation.utils.therapy.parseTherapySessionIdOrFailure
import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingContext

/**
 * Authenticated commands that move authored therapy content through review, publication, and archival.
 *
 * Authorization and lifecycle rules remain in the application and domain layers.
 */
fun Routing.managedTherapyLifecycleRoutes(
    submitTherapyContentForReviewUseCase: SubmitTherapyContentForReviewUseCase,
    returnTherapyContentToDraftUseCase: ReturnTherapyContentToDraftUseCase,
    publishTherapyContentUseCase: PublishTherapyContentUseCase,
    archiveTherapyContentUseCase: ArchiveTherapyContentUseCase,
) {

    // POST /management/therapy-sessions/{therapySessionId}/lifecycle/submit-for-review
    post<ManagedTherapyRoutesPath.Lifecycle.SubmitForReview> { path ->
        executeTherapyLifecycleTransition(
            operation = "submitTherapyContentForReview",
            rawTherapySessionId = path.parent.therapySessionId,
        ) { actorId, therapySessionId ->
            submitTherapyContentForReviewUseCase(
                actorId = actorId,
                therapySessionId = therapySessionId,
            )
        }
    }

    // POST /management/therapy-sessions/{therapySessionId}/lifecycle/return-to-draft
    post<ManagedTherapyRoutesPath.Lifecycle.ReturnToDraft> { path ->
        executeTherapyLifecycleTransition(
            operation = "returnTherapyContentToDraft",
            rawTherapySessionId = path.parent.therapySessionId,
        ) { actorId, therapySessionId ->
            returnTherapyContentToDraftUseCase(
                actorId = actorId,
                therapySessionId = therapySessionId,
            )
        }
    }

    // POST /management/therapy-sessions/{therapySessionId}/lifecycle/publish
    post<ManagedTherapyRoutesPath.Lifecycle.Publish> { path ->
        executeTherapyLifecycleTransition(
            operation = "publishTherapyContent",
            rawTherapySessionId = path.parent.therapySessionId,
        ) { actorId, therapySessionId ->
            publishTherapyContentUseCase(
                actorId = actorId,
                therapySessionId = therapySessionId,
            )
        }
    }

    // POST /management/therapy-sessions/{therapySessionId}/lifecycle/archive
    post<ManagedTherapyRoutesPath.Lifecycle.Archive> { path ->
        executeTherapyLifecycleTransition(
            operation = "archiveTherapyContent",
            rawTherapySessionId = path.parent.therapySessionId,
        ) { actorId, therapySessionId ->
            archiveTherapyContentUseCase(
                actorId = actorId,
                therapySessionId = therapySessionId,
            )
        }
    }
}

/**
 * Applies the common authenticated route behavior shared by lifecycle commands.
 */
private suspend fun RoutingContext.executeTherapyLifecycleTransition(
    operation: String,
    rawTherapySessionId: String,
    transition: suspend (UserId, TherapySessionId) -> ResultType<Unit, DataError>,
) {
    call.preventTherapyMutationCaching()

    val actorId = authenticatedUserIdOrRespondUnauthorized() ?: return

    val therapySessionId = when (val result = parseTherapySessionIdOrFailure(
        operation = operation,
        rawTherapySessionId = rawTherapySessionId,
    )) {
        is ResultType.Success -> result.data

        is ResultType.Failure -> {
            respondWithDataError(result.error)
            return
        }
    }

    when (val result = transition(actorId, therapySessionId)) {
        is ResultType.Success -> call.respond(HttpStatusCode.NoContent)
        is ResultType.Failure -> respondWithDataError(result.error)
    }
}
