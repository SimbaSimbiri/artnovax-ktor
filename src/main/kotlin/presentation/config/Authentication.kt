package com.simbiri.presentation.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.simbiri.application.auth.ValidateAccessTokenSessionUseCase
import com.simbiri.domain.model.common.UserId
import com.simbiri.presentation.auth.AuthenticatedUserPrincipal
import com.simbiri.presentation.auth.JWT_SESSION_VERSION_CLAIM
import com.simbiri.presentation.auth.JwtSettings
import com.simbiri.presentation.routes.dto.auth.AuthenticationErrorResponseDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import org.koin.ktor.ext.getKoin
import java.util.UUID

/**
 * Name used when protecting an ArtNovaX route through authenticate(...).
 */
const val JWT_AUTH_PROVIDER = "artnovax-jwt"

/**
 * Installs bearer-token authentication for protected ArtNovaX routes.
 *
 * Tokens are accepted only when:
 * - the HMAC signature is valid;
 * - issuer and audience match this service;
 * - temporal JWT claims are valid;
 * - the subject contains a valid User UUID.
 */
fun Application.configureAuthentication(
    settings: JwtSettings? = null,
    validateAccessTokenSession: (suspend (
        userId: UserId,
        sessionVersion: Long,
    ) -> Boolean)? = null,
) {
    val resolvedSettings = settings ?: getKoin().get<JwtSettings>()

    val verifier = JWT.require(
        Algorithm.HMAC256(resolvedSettings.secret)
    ).withIssuer(resolvedSettings.issuer).withAudience(resolvedSettings.audience).build()

    val validationUseCase = if (validateAccessTokenSession == null) {
        getKoin().get<ValidateAccessTokenSessionUseCase>()
    } else {
        null
    }

    val resolvedSessionValidator: suspend (
        UserId,
        Long,
    ) -> Boolean = validateAccessTokenSession ?: {
            userId,
            sessionVersion,
        ->

        requireNotNull(
            validationUseCase
        )(
            userId,
            sessionVersion,
        )
    }

    install(Authentication) {
        jwt(JWT_AUTH_PROVIDER) {
            realm = resolvedSettings.realm
            this.verifier(verifier)

            validate { credential ->

                // JWT subject is the identity value trusted by presentation.
                val rawSubject = credential.payload.subject ?: return@validate null

                val userUuid = runCatching {
                    UUID.fromString(
                        rawSubject
                    )
                }.getOrNull() ?: return@validate null

                val sessionVersion = credential.payload.getClaim(
                    JWT_SESSION_VERSION_CLAIM
                ).asLong() ?: return@validate null

                if (sessionVersion <= 0L) {
                    return@validate null
                }

                val userId = UserId(userUuid)

                val sessionIsCurrent = try {
                    resolvedSessionValidator(
                        userId,
                        sessionVersion,
                    )
                } catch (_: Exception) {
                    /*
                    * Authentication must fail closed when persistence or
                    * validation infrastructure fails unexpectedly.
                    */
                    false
                }

                if (!sessionIsCurrent) {
                    return@validate null
                }

                AuthenticatedUserPrincipal(
                    userId = userId
                )
            }
            // because we return a serializable response, we will have to configure Serialization before Authentication
            challenge { _, _ ->
                /*
                 * We don't expose why authentication failed i.e. because of
                 * expiration, issuer, audience, signature, or subject.
                 */
                call.respond(
                    status = HttpStatusCode.Unauthorized,
                    message = AuthenticationErrorResponseDto(
                        message = "A valid bearer access token is required."
                    ),
                )
            }
        }
    }
}
