package com.simbiri.presentation.routes

import com.simbiri.application.therapy.query.GetLatestManagedTherapySessionVersionUseCase
import com.simbiri.application.therapy.query.GetManagedTherapySessionByIdUseCase
import com.simbiri.application.therapy.query.GetManagedTherapySessionsUseCase
import com.simbiri.domain.util.ResultType
import com.simbiri.domain.util.onFailure
import com.simbiri.domain.util.onSuccess
import com.simbiri.presentation.auth.authenticatedUserIdOrRespondUnauthorized
import com.simbiri.presentation.routes.dto.therapy.management.toManagedResponseDto
import com.simbiri.presentation.routes.dto.therapy.management.toManagedSummaryResponseDtos
import com.simbiri.presentation.routes.path.ManagedTherapyRoutesPath
import com.simbiri.presentation.utils.respondWithDataError
import com.simbiri.presentation.utils.therapy.parseTherapySessionIdOrFailure
import com.simbiri.presentation.utils.therapy.parseTherapySessionSeriesIdOrFailure
import com.simbiri.presentation.utils.therapy.toManagedTherapyFiltersOrFailure
import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing

/**
 * Authenticated therapy-content management queries.
 *
 * The route layer establishes the actor identity from the JWT principal.
 * Content ownership and role capabilities remain enforced by application
 * use cases and domain access policies.
 */
fun Routing.managedTherapyRoutes(
    getManagedTherapySessionsUseCase: GetManagedTherapySessionsUseCase,
    getManagedTherapySessionByIdUseCase: GetManagedTherapySessionByIdUseCase,
    getLatestManagedTherapySessionVersionUseCase: GetLatestManagedTherapySessionVersionUseCase,
) {

    // GET /management/therapy-sessions
    get<ManagedTherapyRoutesPath> { path ->
        val actorId = authenticatedUserIdOrRespondUnauthorized() ?: return@get

        val filters = when (val parsed = path.toManagedTherapyFiltersOrFailure(
            operation = "getManagedTherapySessions",
        )) {
            is ResultType.Success -> parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@get
            }
        }

        getManagedTherapySessionsUseCase(
            actorId = actorId,
            filters = filters,
        ).onSuccess { sessions ->
                call.respond(
                    status = HttpStatusCode.OK,
                    message = sessions.toManagedSummaryResponseDtos(),
                )
            }.onFailure { error ->
                respondWithDataError(error)
            }
    }

    // GET /management/therapy-sessions/{therapySessionId}
    get<ManagedTherapyRoutesPath.ById> { path ->
        val actorId = authenticatedUserIdOrRespondUnauthorized() ?: return@get

        val therapySessionId = when (val parsed = parseTherapySessionIdOrFailure(
            operation = "getManagedTherapySessionById",
            rawTherapySessionId = path.therapySessionId,
        )) {
            is ResultType.Success -> parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@get
            }
        }

        getManagedTherapySessionByIdUseCase(
            actorId = actorId,
            therapySessionId = therapySessionId,
        ).onSuccess { session ->
                call.respond(
                    status = HttpStatusCode.OK,
                    message = session.toManagedResponseDto(),
                )
            }.onFailure { error ->
                respondWithDataError(error)
            }
    }

    // GET /management/therapy-sessions/series/{seriesId}/latest
    get<ManagedTherapyRoutesPath.LatestVersion> { path ->
        val actorId = authenticatedUserIdOrRespondUnauthorized() ?: return@get

        val seriesId = when (val parsed = parseTherapySessionSeriesIdOrFailure(
            operation = "getLatestManagedTherapySessionVersion",
            rawSeriesId = path.seriesId,
        )) {
            is ResultType.Success -> parsed.data

            is ResultType.Failure -> {
                respondWithDataError(parsed.error)
                return@get
            }
        }

        getLatestManagedTherapySessionVersionUseCase(
            actorId = actorId,
            seriesId = seriesId,
        ).onSuccess { session ->
                call.respond(
                    status = HttpStatusCode.OK,
                    message = session.toManagedResponseDto(),
                )
            }.onFailure { error ->
                respondWithDataError(error)
            }
    }
}
