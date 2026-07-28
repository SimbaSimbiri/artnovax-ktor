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

        private const val MINIMUM_SECRET_BYTES = 32
    }
}
