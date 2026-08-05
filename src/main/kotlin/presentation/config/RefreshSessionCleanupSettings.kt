package com.simbiri.presentation.config

/**
 * Configuration for refresh-session retention and periodic cleanup.
 */
data class RefreshSessionCleanupSettings(
    val enabled: Boolean = true,
    val initialDelaySeconds: Long = DEFAULT_INITIAL_DELAY_SECONDS,
    val intervalSeconds: Long = DEFAULT_INTERVAL_SECONDS,
    val retentionSeconds: Long = DEFAULT_RETENTION_SECONDS,
    val batchSize: Int = DEFAULT_BATCH_SIZE,
    val maxBatchesPerRun: Int = DEFAULT_MAX_BATCHES_PER_RUN,
) {

    init {
        require(
            initialDelaySeconds in 0L..MAXIMUM_INITIAL_DELAY_SECONDS
        ) {
            "Refresh-session cleanup initial delay is invalid."
        }

        require(
            intervalSeconds in MINIMUM_INTERVAL_SECONDS..MAXIMUM_INTERVAL_SECONDS
        ) {
            "Refresh-session cleanup interval is invalid."
        }

        require(
            retentionSeconds in 0L..MAXIMUM_RETENTION_SECONDS
        ) {
            "Refresh-session cleanup retention is invalid."
        }

        require(
            batchSize in 1..MAXIMUM_BATCH_SIZE
        ) {
            "Refresh-session cleanup batch size is invalid."
        }

        require(
            maxBatchesPerRun in 1..MAXIMUM_BATCHES_PER_RUN
        ) {
            "Refresh-session cleanup batch count is invalid."
        }
    }

    companion object {

        fun fromEnvironment(
            environment: Map<String, String> = System.getenv(),
        ): RefreshSessionCleanupSettings = RefreshSessionCleanupSettings(
            enabled = environment.readBoolean(
                name = "REFRESH_SESSION_CLEANUP_ENABLED",

                defaultValue = true,
            ),

            initialDelaySeconds = environment.readLong(
                name = "REFRESH_SESSION_CLEANUP_INITIAL_DELAY_SECONDS",

                defaultValue = DEFAULT_INITIAL_DELAY_SECONDS,
            ),

            intervalSeconds = environment.readLong(
                name = "REFRESH_SESSION_CLEANUP_INTERVAL_SECONDS",

                defaultValue = DEFAULT_INTERVAL_SECONDS,
            ),

            retentionSeconds = environment.readLong(
                name = "REFRESH_SESSION_RETENTION_SECONDS",

                defaultValue = DEFAULT_RETENTION_SECONDS,
            ),

            batchSize = environment.readInt(
                name = "REFRESH_SESSION_CLEANUP_BATCH_SIZE",

                defaultValue = DEFAULT_BATCH_SIZE,
            ),

            maxBatchesPerRun = environment.readInt(
                name = "REFRESH_SESSION_CLEANUP_MAX_BATCHES",

                defaultValue = DEFAULT_MAX_BATCHES_PER_RUN,
            ),
        )

        private fun Map<String, String>.readBoolean(
            name: String,
            defaultValue: Boolean,
        ): Boolean {
            val value = this[name]?.trim()?.takeIf(
                    String::isNotEmpty
                ) ?: return defaultValue

            return value.toBooleanStrictOrNull() ?: error(
                "$name must be either true or false."
            )
        }

        private fun Map<String, String>.readLong(
            name: String,
            defaultValue: Long,
        ): Long {
            val value = this[name]?.trim()?.takeIf(
                    String::isNotEmpty
                ) ?: return defaultValue

            return value.toLongOrNull() ?: error(
                "$name must contain a valid whole number."
            )
        }

        private fun Map<String, String>.readInt(
            name: String,
            defaultValue: Int,
        ): Int {
            val value = this[name]?.trim()?.takeIf(
                    String::isNotEmpty
                ) ?: return defaultValue

            return value.toIntOrNull() ?: error(
                "$name must contain a valid whole number."
            )
        }

        private const val DEFAULT_INITIAL_DELAY_SECONDS = 60L
        private const val DEFAULT_INTERVAL_SECONDS = 24L * 60L * 60L
        private const val DEFAULT_RETENTION_SECONDS = 7L * 24L * 60L * 60L
        private const val DEFAULT_BATCH_SIZE = 500
        private const val DEFAULT_MAX_BATCHES_PER_RUN = 20
        private const val MAXIMUM_INITIAL_DELAY_SECONDS = 60L * 60L
        private const val MINIMUM_INTERVAL_SECONDS = 5L * 60L
        private const val MAXIMUM_INTERVAL_SECONDS = 7L * 24L * 60L * 60L
        private const val MAXIMUM_RETENTION_SECONDS = 90L * 24L * 60L * 60L
        private const val MAXIMUM_BATCH_SIZE = 5_000
        private const val MAXIMUM_BATCHES_PER_RUN = 100
    }
}
