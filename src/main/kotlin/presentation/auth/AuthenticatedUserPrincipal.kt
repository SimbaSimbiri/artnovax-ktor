package com.simbiri.presentation.auth

import com.simbiri.domain.model.common.UserId
import io.ktor.server.auth.Principal

/**
 * Identity established after a bearer token passes JWT verification.
 */
data class AuthenticatedUserPrincipal(
    val userId: UserId,
): Principal
