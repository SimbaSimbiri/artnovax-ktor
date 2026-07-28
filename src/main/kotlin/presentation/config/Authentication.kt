package com.simbiri.presentation.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.simbiri.domain.model.common.UserId
import com.simbiri.presentation.auth.AuthenticatedUserPrincipal
import com.simbiri.presentation.auth.JwtSettings
import com.simbiri.presentation.routes.dto.auth.AuthenticationErrorResponseDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
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
    settings: JwtSettings = JwtSettings.fromEnvironment(),
) {
    val verifier = JWT.require(
            Algorithm.HMAC256(settings.secret)
        ).withIssuer(settings.issuer).withAudience(settings.audience).build()

    install(Authentication) {
        jwt(JWT_AUTH_PROVIDER) {
            realm = settings.realm
            this.verifier(verifier)

            validate { credential ->

                //JWT subject is the only identity value trusted by presentation.
                val rawSubject = credential.payload.subject ?: return@validate null

                val userUuid = runCatching {
                    UUID.fromString(rawSubject)
                }.getOrNull() ?: return@validate null

                AuthenticatedUserPrincipal(
                    userId = UserId(userUuid)
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
