package com.simbiri.domain.model.auth

import com.simbiri.domain.model.common.Timestamp

/**
 * Newly generated opaque refresh-token material.
 *
 * value is returned to the authenticated client once.
 * hash is the only representation that may be persisted.
 */
data class IssuedRefreshToken(
    val value: String,
    val hash: String,
    val expiresAt: Timestamp,
) {

    init {
        require(value.isNotBlank()) {
            "Issued refresh-token value must not be blank."
        }

        require(
            hash.matches(
                Regex("^[0-9a-f]{64}$")
            )
        ) {
            "Issued refresh-token hash must be a lowercase SHA-256 digest."
        }
    }

    override fun toString(): String =
        "IssuedRefreshToken(value=<redacted>, hash=<redacted>, expiresAt=$expiresAt)"
}
