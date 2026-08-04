package com.simbiri.domain.security

/**
 * Lifetime configuration for opaque refresh tokens.
 */
data class RefreshTokenSettings(
    val ttlSeconds: Long = DEFAULT_TTL_SECONDS,
) {

    init {
        require(
            ttlSeconds in MINIMUM_TTL_SECONDS..MAXIMUM_TTL_SECONDS
        ) {
            "Refresh-token lifetime must be between $MINIMUM_TTL_SECONDS and $MAXIMUM_TTL_SECONDS seconds."
        }
    }

    companion object {

        fun fromEnvironment(
            environment: Map<String, String> = System.getenv(),
        ): RefreshTokenSettings {
            val configuredTtl = environment[REFRESH_TOKEN_TTL_ENV]?.trim()?.takeIf(
                    String::isNotEmpty
                )?.let { value ->
                    value.toLongOrNull() ?: error(
                        "$REFRESH_TOKEN_TTL_ENV must contain a valid whole number."
                    )
                } ?: DEFAULT_TTL_SECONDS

            return RefreshTokenSettings(
                ttlSeconds = configuredTtl
            )
        }

        private const val REFRESH_TOKEN_TTL_ENV = "REFRESH_TOKEN_TTL_SECONDS"

        private const val DEFAULT_TTL_SECONDS = 30L * 24L * 60L * 60L

        private const val MINIMUM_TTL_SECONDS = 60L * 60L

        private const val MAXIMUM_TTL_SECONDS = 90L * 24L * 60L * 60L
    }
}
