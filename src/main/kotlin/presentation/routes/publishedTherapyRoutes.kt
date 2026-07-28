package com.simbiri.presentation.routes

import com.simbiri.application.therapy.query.GetPublishedTherapySessionByIdUseCase
import com.simbiri.application.therapy.query.GetPublishedTherapySessionsUseCase
import com.simbiri.domain.util.ResultType
import com.simbiri.domain.util.onFailure
import com.simbiri.domain.util.onSuccess
import com.simbiri.presentation.routes.dto.therapy.published.toPublishedResponseDto
import com.simbiri.presentation.routes.dto.therapy.published.toPublishedSummaryResponseDtos
import com.simbiri.presentation.routes.path.PublishedTherapyRoutesPath
import com.simbiri.presentation.utils.respondWithDataError
import com.simbiri.presentation.utils.therapy.parseTherapySessionIdOrFailure
import com.simbiri.presentation.utils.therapy.toPublishedTherapyFiltersOrFailure
import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing

/**
 * Public read-only therapy-session catalogue.
 *
 * These routes expose PUBLISHED content only. Draft, review, and archived
 * content remain accessible exclusively through authenticated management
 * workflows.
 */
fun Routing.publishedTherapyRoutes(
    getPublishedTherapySessionsUseCase:
    GetPublishedTherapySessionsUseCase,
    getPublishedTherapySessionByIdUseCase:
    GetPublishedTherapySessionByIdUseCase,
) {

    // GET /therapy-sessions?goal={value}&intensity={value}&locale={value}
    get<PublishedTherapyRoutesPath> { path ->
        val filters =
            when (
                val parsed =
                    path.toPublishedTherapyFiltersOrFailure(
                        operation = "getPublishedTherapySessions",
                    )
            ) {
                is ResultType.Success ->
                    parsed.data

                is ResultType.Failure -> {
                    respondWithDataError(parsed.error)
                    return@get
                }
            }

        getPublishedTherapySessionsUseCase(
            goal = filters.goal,
            intensity = filters.intensity,
            locale = filters.locale,
        )
            .onSuccess { sessions ->
                call.respond(
                    status = HttpStatusCode.OK,
                    message =
                        sessions
                            .toPublishedSummaryResponseDtos(),
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }

    // GET /therapy-sessions/{therapySessionId}
    get<PublishedTherapyRoutesPath.ById> { path ->
        val therapySessionId =
            when (
                val parsed =
                    parseTherapySessionIdOrFailure(
                        operation = "getPublishedTherapySessionById",
                        rawTherapySessionId =
                            path.therapySessionId,
                    )
            ) {
                is ResultType.Success ->
                    parsed.data

                is ResultType.Failure -> {
                    respondWithDataError(parsed.error)
                    return@get
                }
            }

        getPublishedTherapySessionByIdUseCase(
            therapySessionId
        )
            .onSuccess { session ->
                call.respond(
                    status = HttpStatusCode.OK,
                    message =
                        session.toPublishedResponseDto(),
                )
            }
            .onFailure { error ->
                respondWithDataError(error)
            }
    }
}
