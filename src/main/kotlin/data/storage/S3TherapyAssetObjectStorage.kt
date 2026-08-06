package com.simbiri.data.storage

import com.simbiri.domain.storage.TherapyAssetDownloadGrant
import com.simbiri.domain.storage.TherapyAssetObjectStorage
import com.simbiri.domain.storage.TherapyStoredObject
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ChecksumMode
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Clock
import java.time.Duration
import java.util.Base64

class S3TherapyAssetObjectStorage(
    private val bucketName: String,
    private val s3Client: S3Client,
    private val presigner: S3Presigner,
    private val clock: Clock,
    private val downloadDuration: Duration = Duration.ofMinutes(10),
) : TherapyAssetObjectStorage {

    override suspend fun inspectObject(
        storageKey: String,
    ): ResultType<TherapyStoredObject, DataError> {
        storageConfigurationError()?.let { error ->
            return ResultType.Failure(error)
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = s3Client.headObject(
                    HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(storageKey)
                        .checksumMode(ChecksumMode.ENABLED)
                        .build()
                )

                ResultType.Success(
                    TherapyStoredObject(
                        storageKey = storageKey,
                        mimeType = response.contentType(),
                        sizeBytes = response.contentLength(),
                        sha256 = response.checksumSHA256()?.let(::sha256Base64ToHex),
                    )
                )
            } catch (exception: S3Exception) {
                ResultType.Failure(exception.toDataError("inspect therapy asset"))
            } catch (exception: Exception) {
                ResultType.Failure(
                    DataError.UnknownError(
                        cause = exception.message ?: "Unable to inspect the therapy asset."
                    )
                )
            }
        }
    }

    override suspend fun createDownloadGrant(
        storageKey: String,
    ): ResultType<TherapyAssetDownloadGrant, DataError> {
        storageConfigurationError()?.let { error ->
            return ResultType.Failure(error)
        }

        return try {
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .build()

            val presignedRequest = presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                    .signatureDuration(downloadDuration)
                    .getObjectRequest(getObjectRequest)
                    .build()
            )

            ResultType.Success(
                TherapyAssetDownloadGrant(
                    downloadUrl = presignedRequest.url().toExternalForm(),
                    expiresAt = clock.instant().plus(downloadDuration),
                )
            )
        } catch (exception: S3Exception) {
            ResultType.Failure(exception.toDataError("sign therapy asset download"))
        } catch (exception: Exception) {
            ResultType.Failure(
                DataError.UnknownError(
                    cause = exception.message ?: "Unable to create the therapy asset download URL."
                )
            )
        }
    }

    override suspend fun deleteObject(
        storageKey: String,
    ): ResultType<Unit, DataError> {
        storageConfigurationError()?.let { error ->
            return ResultType.Failure(error)
        }

        return withContext(Dispatchers.IO) {
            try {
                s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(storageKey)
                        .build()
                )

                ResultType.Success(Unit)
            } catch (exception: S3Exception) {
                ResultType.Failure(exception.toDataError("delete replaced therapy asset"))
            } catch (exception: Exception) {
                ResultType.Failure(
                    DataError.UnknownError(
                        cause = exception.message ?: "Unable to delete the therapy asset."
                    )
                )
            }
        }
    }

    private fun storageConfigurationError(): DataError.UnknownError? =
        if (bucketName.isBlank()) {
            DataError.UnknownError(
                cause = "Therapy asset storage is not configured."
            )
        } else {
            null
        }

    private fun S3Exception.toDataError(operation: String): DataError =
        if (statusCode() == 404) {
            DataError.NotFound
        } else {
            val errorMessage = awsErrorDetails()?.errorMessage() ?: message ?: "Unknown S3 error"

            DataError.UnknownError(
                cause = "$operation failed. statusCode=${statusCode()}, message=$errorMessage"
            )
        }
}

internal fun sha256Base64ToHex(value: String): String =
    Base64.getDecoder()
        .decode(value)
        .joinToString(separator = "") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
