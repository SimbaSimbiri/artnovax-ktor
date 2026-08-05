package com.simbiri.presentation.config

import io.ktor.server.application.log
import com.simbiri.application.auth.CleanupRefreshSessionsUseCase
import com.simbiri.domain.util.ResultType
import io.ktor.server.application.Application
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

/**
 * Runs bounded refresh-session cleanup for the lifetime of the application.
 */
fun Application.configureRefreshSessionCleanup() {
    val settings = RefreshSessionCleanupSettings.fromEnvironment()

    if (!settings.enabled) {
        log.info(
            "Refresh-session cleanup is disabled."
        )

        return
    }

    val cleanupRefreshSessionsUseCase: CleanupRefreshSessionsUseCase by inject()

    val clock: Clock by inject()

    launch(
        CoroutineName(
            "refresh-session-cleanup"
        )
    ) {
        delay(
            (settings.initialDelaySeconds * MILLISECONDS_PER_SECOND).milliseconds
        )

        while (isActive) {
            try {
                val expiredBefore = Instant.now(clock).minusSeconds(
                        settings.retentionSeconds
                    )

                when (val result = cleanupRefreshSessionsUseCase(
                    expiredBefore = expiredBefore,
                    batchSize = settings.batchSize,
                    maxBatches = settings.maxBatchesPerRun,
                )) {
                    is ResultType.Success -> {
                        log.info(
                            "Refresh-session cleanup deleted ${result.data.deletedCount} rows across "
                                    + "${result.data.processedBatches} batches."
                        )
                    }

                    is ResultType.Failure -> {
                        log.error(
                            "Refresh-session cleanup failed: " + result.error
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {/*
                 * One failed execution must not permanently stop future
                 * cleanup attempts.
                 */
                log.error(
                    "Unexpected refresh-session cleanup failure.",
                    e,
                )
            }

            delay(
                (settings.intervalSeconds * MILLISECONDS_PER_SECOND).milliseconds
            )
        }
    }
}

private const val MILLISECONDS_PER_SECOND = 1_000L
