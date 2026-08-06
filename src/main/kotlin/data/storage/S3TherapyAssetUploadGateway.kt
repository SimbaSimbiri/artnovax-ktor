package com.simbiri.data.storage

import com.simbiri.domain.storage.TherapyAssetUploadGateway
import com.simbiri.domain.storage.TherapyAssetUploadGrant
import com.simbiri.domain.storage.TherapyAssetUploadSpecification
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Clock
import java.time.Duration

class S3TherapyAssetUploadGateway(
    private val bucketName: String,
    private val presigner: S3Presigner,
    private val clock: Clock,
    private val signatureDuration: Duration = Duration.ofMinutes(10),
) : TherapyAssetUploadGateway {

    override fun createUploadGrant(
        specification: TherapyAssetUploadSpecification,
    ): ResultType<TherapyAssetUploadGrant, DataError> {
        if (bucketName.isBlank()) {
            return ResultType.Failure(
                DataError.UnknownError(
                    cause = "Therapy asset storage is not configured."
                )
            )
        }

        return try {
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(specification.storageKey)
                .contentType(specification.mimeType)
                .contentLength(specification.sizeBytes)
                .metadata(
                    mapOf(
                        SHA_256_METADATA_KEY to specification.sha256,
                    )
                )
                .build()

            val presignedRequest = presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                    .signatureDuration(signatureDuration)
                    .putObjectRequest(putObjectRequest)
                    .build()
            )

            val requiredHeaders = presignedRequest.httpRequest()
                .headers()
                .filterKeys { headerName ->
                    !headerName.equals("host", ignoreCase = true)
                }
                .mapValues { (_, values) ->
                    values.joinToString(",")
                }

            ResultType.Success(
                TherapyAssetUploadGrant(
                    uploadUrl = presignedRequest.url().toExternalForm(),
                    storageKey = specification.storageKey,
                    expiresAt = clock.instant().plus(signatureDuration),
                    requiredHeaders = requiredHeaders,
                )
            )
        } catch (exception: Exception) {
            ResultType.Failure(
                DataError.UnknownError(
                    cause = exception.message ?: "Unable to create the therapy asset upload URL."
                )
            )
        }
    }

    private companion object {
        const val SHA_256_METADATA_KEY = "sha256"
    }
}
