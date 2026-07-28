package com.simbiri.presentation.routes.path

import io.ktor.resources.Resource

/**
 * Therapy-content authoring, review, and publication collection.
 *
 * The authenticated actor is not supplied through this resource. The
 * route layer must resolve actor from the authentication principal.
 */
@Resource("/management/therapy-sessions")
data class ManagedTherapyRoutesPath(
    val status: String? = null,
    val authorId: String? = null,
    val goal: String? = null,
    val intensity: String? = null,
    val locale: String? = null,
) {

    /**
     * Retrieves one therapy-session version for a management workflow.
     */
    @Resource("{therapySessionId}")
    data class ById(
        val therapySessionId: String,
        val parent: ManagedTherapyRoutesPath =
            ManagedTherapyRoutesPath(),
    )

    /**
     * Retrieves the highest persisted version in one therapy-session
     * series.
     */
    @Resource("series/{seriesId}/latest")
    data class LatestVersion(
        val seriesId: String,
        val parent: ManagedTherapyRoutesPath =
            ManagedTherapyRoutesPath(),
    )
}
