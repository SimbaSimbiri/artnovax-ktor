package com.simbiri.presentation.auth

import java.nio.charset.StandardCharsets

/**
 * Runtime configuration used to verify ArtNovaX access tokens..
 */
data class JwtSettings(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val accessTokenTtlSeconds: Long =
        DEFAULT_ACCESS_TOKEN_TTL_SECONDS,
) {

    init {
        require(secret.isNotBlank()) {
            "JWT secret must not be blank."
        }

        require(
            secret.toByteArray(
                StandardCharsets.UTF_8
            ).size >= MINIMUM_SECRET_BYTES
        ) {
            "JWT secret must contain at least $MINIMUM_SECRET_BYTES bytes."
        }

        require(issuer.isNotBlank()) {
            "JWT issuer must not be blank."
        }

        require(audience.isNotBlank()) {
            "JWT audience must not be blank."
        }

        require(realm.isNotBlank()) {
            "JWT realm must not be blank."
        }

        require(
            accessTokenTtlSeconds in
                    MINIMUM_ACCESS_TOKEN_TTL_SECONDS..
                    MAXIMUM_ACCESS_TOKEN_TTL_SECONDS
        ) {
            "JWT access-token lifetime must be between " +
                    "$MINIMUM_ACCESS_TOKEN_TTL_SECONDS and " +
                    "$MAXIMUM_ACCESS_TOKEN_TTL_SECONDS seconds."
        }
    }

    companion object {

        /**
         * Loads JWT verification settings from environment variables.
         *
         * Only JWT_SECRET is mandatory. Remaining values have stable
         * defaults that may be overridden per environment.
         */
        fun fromEnvironment(
            environment: Map<String, String> = System.getenv(),
        ): JwtSettings = JwtSettings(
            secret = requireEnvironmentValue(
                environment = environment,
                name = "JWT_SECRET",
            ),

            issuer = environmentValueOrDefault(
                environment = environment,
                name = "JWT_ISSUER",
                defaultValue = "artnovax-api",
            ),

            audience = environmentValueOrDefault(
                environment = environment,
                name = "JWT_AUDIENCE",
                defaultValue = "artnovax-mobile",
            ),

            realm = environmentValueOrDefault(
                environment = environment,
                name = "JWT_REALM",
                defaultValue = "ArtNovaX",
            ),
            accessTokenTtlSeconds =
                environmentLongOrDefault(
                    environment = environment,
                    name =
                        "JWT_ACCESS_TOKEN_TTL_SECONDS",
                    defaultValue =
                        DEFAULT_ACCESS_TOKEN_TTL_SECONDS,
                ),
        )

        private fun requireEnvironmentValue(
            environment: Map<String, String>,
            name: String,
        ): String {
            val value = environment[name]

            if (value.isNullOrBlank()) {
                error(
                    "$name must be configured before the ArtNovaX server starts."
                )
            }

            return value
        }

        private fun environmentValueOrDefault(
            environment: Map<String, String>,
            name: String,
            defaultValue: String,
        ): String = environment[name]?.trim()?.takeIf(String::isNotEmpty) ?: defaultValue

        private fun environmentLongOrDefault(
            environment: Map<String, String>,
            name: String,
            defaultValue: Long,
        ): Long {
            val rawValue = environment[name]?.trim()?.takeIf(String::isNotEmpty) ?: return defaultValue

            return rawValue.toLongOrNull() ?: error(
                "$name must contain a valid whole number."
            )
        }

        private const val MINIMUM_SECRET_BYTES = 32
        private const val DEFAULT_ACCESS_TOKEN_TTL_SECONDS = 15L * 60L
        private const val MINIMUM_ACCESS_TOKEN_TTL_SECONDS = 60L
        private const val MAXIMUM_ACCESS_TOKEN_TTL_SECONDS = 60L * 60L
    }
}
