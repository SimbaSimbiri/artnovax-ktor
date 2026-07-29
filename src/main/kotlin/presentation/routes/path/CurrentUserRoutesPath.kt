package com.simbiri.presentation.routes.path

import io.ktor.resources.Resource

/**
 * Profile belonging to the authenticated JWT principal.
 */
@Resource("/me")
class CurrentUserRoutesPath {
    /**
     * Password credential belonging to the current user.
     */
    @Resource("password")
    data class Password(
        val parent: CurrentUserRoutesPath = CurrentUserRoutesPath(),
    )
}