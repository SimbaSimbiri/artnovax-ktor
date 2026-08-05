package com.simbiri.application.auth

import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.repository.RefreshSessionCleanupRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Deletes expired refresh sessions in bounded batches.
 */
class CleanupRefreshSessionsUseCase(
    private val refreshSessionCleanupRepository: RefreshSessionCleanupRepository,
) {

    suspend operator fun invoke(
        expiredBefore: Timestamp,
        batchSize: Int,
        maxBatches: Int,
    ): ResultType<
            RefreshSessionCleanupReport,
            DataError,
            > {
        if (batchSize !in MINIMUM_BATCH_SIZE..MAXIMUM_BATCH_SIZE) {
            return ResultType.Failure(
                DataError.ValidationError(
                    message = "Refresh-session cleanup batchSize must be between $MINIMUM_BATCH_SIZE and $MAXIMUM_BATCH_SIZE."
                )
            )
        }

        if (maxBatches !in MINIMUM_BATCH_COUNT..MAXIMUM_BATCH_COUNT) {
            return ResultType.Failure(
                DataError.ValidationError(
                    message = "Refresh-session cleanup maxBatches must be between $MINIMUM_BATCH_COUNT and $MAXIMUM_BATCH_COUNT."
                )
            )
        }

        var deletedCount = 0

        var processedBatches = 0

        while (processedBatches < maxBatches) {
            val batchResult = refreshSessionCleanupRepository.deleteSessionsExpiredBefore(
                    expiredBefore = expiredBefore,
                    limit = batchSize,
                )

            val batchDeletedCount = when (batchResult) {
                is ResultType.Success -> batchResult.data

                is ResultType.Failure -> return ResultType.Failure(
                    batchResult.error
                )
            }

            if (batchDeletedCount !in 0..batchSize) {
                return ResultType.Failure(
                    DataError.UnknownError(
                        cause = "Refresh-session cleanup repository returned an invalid deleted-row count."
                    )
                )
            }

            deletedCount += batchDeletedCount

            processedBatches += 1


            if (batchDeletedCount < batchSize) {
                break
            }
        }

        return ResultType.Success(
            RefreshSessionCleanupReport(
                deletedCount = deletedCount,
                processedBatches = processedBatches,
            )
        )
    }

    private companion object {
        const val MINIMUM_BATCH_SIZE = 1

        const val MAXIMUM_BATCH_SIZE = 5_000

        const val MINIMUM_BATCH_COUNT = 1

        const val MAXIMUM_BATCH_COUNT = 100
    }
}


