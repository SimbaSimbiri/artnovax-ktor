package com.simbiri.presentation.auth

import com.simbiri.domain.model.common.UserId
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal

/**
 * Returns UserId established by JWT authentication.
 *
 * Protected routes must call this inside an authenticate block.
 */
fun ApplicationCall.authenticatedUserIdOrNull(): UserId? = principal<AuthenticatedUserPrincipal>()?.userId
