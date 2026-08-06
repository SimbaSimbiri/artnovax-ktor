package com.simbiri.presentation.routes

import com.simbiri.application.therapy.asset.ConfirmTherapyAssetUploadUseCase
import com.simbiri.application.therapy.asset.RequestTherapyAssetUploadUseCase
import com.simbiri.domain.util.ResultType
import com.simbiri.presentation.auth.authenticatedUserIdOrRespondUnauthorized
import com.simbiri.presentation.routes.dto.therapy.management.ConfirmTherapyAssetUploadRequestDto
import com.simbiri.presentation.routes.dto.therapy.management.ConfirmedTherapyAssetResponseDto
import com.simbiri.presentation.routes.dto.therapy.management.TherapyAssetUploadRequestDto
import com.simbiri.presentation.routes.dto.therapy.management.toConfirmationRequestOrFailure
import com.simbiri.presentation.routes.dto.therapy.management.toResponseDto
import com.simbiri.presentation.routes.dto.therapy.management.toTherapyAssetUploadRequestOrFailure
import com.simbiri.presentation.routes.path.ManagedTherapyRoutesPath
import com.simbiri.presentation.utils.respondWithDataError
import com.simbiri.presentation.utils.therapy.parseTherapySessionIdOrFailure
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing

fun Routing.managedTherapyAssetUploadRoutes(
    requestTherapyAssetUploadUseCase: RequestTherapyAssetUploadUseCase,
    confirmTherapyAssetUploadUseCase: ConfirmTherapyAssetUploadUseCase,
) {

    // POST /management/therapy-sessions/{therapySessionId}/asset-uploads
    post<ManagedTherapyRoutesPath.AssetUploads> { path ->
        call.preventTherapyMutationCaching()

        val actorId = authenticatedUserIdOrRespondUnauthorized() ?: return@post

        val therapySessionId = when (
            val result = parseTherapySessionIdOrFailure(
                operation = "requestTherapyAssetUpload",
                rawTherapySessionId = path.therapySessionId,
            )
        ) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> {
                respondWithDataError(result.error)
                return@post
            }
        }

        val requestDto = call.receive<TherapyAssetUploadRequestDto>()

        val request = when (
            val result = requestDto.toTherapyAssetUploadRequestOrFailure(therapySessionId)
        ) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> {
                respondWithDataError(result.error)
                return@post
            }
        }

        when (
            val result = requestTherapyAssetUploadUseCase(
                actorId = actorId,
                request = request,
            )
        ) {
            is ResultType.Success -> {
                call.respond(
                    status = HttpStatusCode.Created,
                    message = result.data.toResponseDto(),
                )
            }

            is ResultType.Failure -> respondWithDataError(result.error)
        }
    }

    // POST /management/therapy-sessions/{therapySessionId}/asset-uploads/confirm
    post<ManagedTherapyRoutesPath.AssetUploads.Confirm> { path ->
        call.preventTherapyMutationCaching()

        val actorId = authenticatedUserIdOrRespondUnauthorized() ?: return@post

        val therapySessionId = when (
            val result = parseTherapySessionIdOrFailure(
                operation = "confirmTherapyAssetUpload",
                rawTherapySessionId = path.parent.therapySessionId,
            )
        ) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> {
                respondWithDataError(result.error)
                return@post
            }
        }

        val requestDto = call.receive<ConfirmTherapyAssetUploadRequestDto>()

        val request = when (
            val result = requestDto.toConfirmationRequestOrFailure(therapySessionId)
        ) {
            is ResultType.Success -> result.data

            is ResultType.Failure -> {
                respondWithDataError(result.error)
                return@post
            }
        }

        when (
            val result = confirmTherapyAssetUploadUseCase(
                actorId = actorId,
                request = request,
            )
        ) {
            is ResultType.Success -> {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = ConfirmedTherapyAssetResponseDto(
                        therapyAssetId = result.data.value.toString(),
                    ),
                )
            }

            is ResultType.Failure -> respondWithDataError(result.error)
        }
    }
}
