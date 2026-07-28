package com.simbiri.domain.model.auth

import com.simbiri.domain.model.common.Timestamp

/**
 * Access token created after successful authentication.
 */
data class IssuedAccessToken(
    val value: String,
    val expiresAt: Timestamp,
)
