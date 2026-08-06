package com.simbiri.presentation.routes

import com.simbiri.application.therapy.asset.GetManagedTherapyAssetDownloadUseCase
import com.simbiri.application.therapy.asset.GetPublishedTherapyAssetDownloadUseCase
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.auth.authenticatedUserIdOrRespondUnauthorized
import com.simbiri.presentation.routes.dto.therapy.toResponseDto
import com.simbiri.presentation.routes.path.ManagedTherapyRoutesPath
import com.simbiri.presentation.routes.path.PublishedTherapyRoutesPath
import com.simbiri.presentation.utils.respondWithDataError
import com.simbiri.presentation.utils.therapy.parseTherapyAssetIdOrFailure
import com.simbiri.presentation.utils.therapy.parseTherapySessionIdOrFailure
import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing

fun Routing.publishedTherapyAssetRoutes(
    getPublishedTherapyAssetDownloadUseCase: GetPublishedTherapyAssetDownloadUseCase,
) {

    // GET /therapy-sessions/{therapySessionId}/assets/{therapyAssetId}/download
    get<PublishedTherapyRoutesPath.Assets.Download> { path ->
        call.preventTherapyMutationCaching()

        val therapySessionId = when (
            val result = parseTherapySessionIdOrFailure(
                operation = "getPublishedTherapyAssetDownload",
                rawTherapySessionId = path.parent.therapySessionId,
            )
        ) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> {
                respondWithDataError(result.error)
                return@get
            }
        }

        val therapyAssetId = when (
            val result = parseTherapyAssetIdOrFailure(
                operation = "getPublishedTherapyAssetDownload",
                rawTherapyAssetId = path.therapyAssetId,
            )
        ) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> {
                respondWithDataError(result.error)
                return@get
            }
        }

        when (
            val result = getPublishedTherapyAssetDownloadUseCase(
                therapySessionId = therapySessionId,
                therapyAssetId = therapyAssetId,
            )
        ) {
            is ResultType.Success -> {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = result.data.toResponseDto(),
                )
            }

            is ResultType.Failure -> respondWithDataError(result.error)
        }
    }
}

fun Routing.managedTherapyAssetRoutes(
    getManagedTherapyAssetDownloadUseCase: GetManagedTherapyAssetDownloadUseCase,
) {

    // GET /management/therapy-sessions/{therapySessionId}/assets/{therapyAssetId}/download
    get<ManagedTherapyRoutesPath.Assets.Download> { path ->
        call.preventTherapyMutationCaching()

        val actorId = authenticatedUserIdOrRespondUnauthorized() ?: return@get

        val therapySessionId = when (
            val result = parseTherapySessionIdOrFailure(
                operation = "getManagedTherapyAssetDownload",
                rawTherapySessionId = path.parent.therapySessionId,
            )
        ) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> {
                respondWithDataError(result.error)
                return@get
            }
        }

        val therapyAssetId = when (
            val result = parseTherapyAssetIdOrFailure(
                operation = "getManagedTherapyAssetDownload",
                rawTherapyAssetId = path.therapyAssetId,
            )
        ) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> {
                respondWithDataError(result.error)
                return@get
            }
        }

        when (
            val result = getManagedTherapyAssetDownloadUseCase(
                actorId = actorId,
                therapySessionId = therapySessionId,
                therapyAssetId = therapyAssetId,
            )
        ) {
            is ResultType.Success -> {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = result.data.toResponseDto(),
                )
            }

            is ResultType.Failure -> respondWithDataError(result.error)
        }
    }
}
