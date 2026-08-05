package com.simbiri.application.auth

/**
 * Summary of one bounded cleanup execution.
 */
data class RefreshSessionCleanupReport(
    val deletedCount: Int,
    val processedBatches: Int,
)