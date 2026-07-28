package com.simbiri.domain.model.auth

/**
 * Password-hashing algorithms supported.
 */
enum class PasswordHashAlgorithm {
    ARGON2ID;

    companion object {
        /**
         * Reconstructs algorithm stored by the persistence layer.
         */
        fun fromStorageValue(
            value: String,
        ): PasswordHashAlgorithm = entries.firstOrNull { algorithm ->
            algorithm.name == value
        } ?: error(
            "Unsupported persisted password-hash algorithm: $value."
        )
    }
}
