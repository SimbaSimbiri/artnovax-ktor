package com.simbiri.presentation.routes.path

import io.ktor.resources.Resource

/**
 * Profile belonging to the authenticated JWT principal.
 */
@Resource("/me")
class CurrentUserRoutesPath
