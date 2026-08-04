package com.simbiri.domain.security

import com.simbiri.domain.model.auth.IssuedAccessToken
import com.simbiri.domain.model.common.UserId

/**
 * Creates a signed access token for one authenticated user.
 */
interface AccessTokenIssuer {

    fun issue(
        userId: UserId,
        sessionVersion: Long,
    ): IssuedAccessToken
}
