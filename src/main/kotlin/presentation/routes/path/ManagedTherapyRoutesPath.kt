package com.simbiri.presentation.routes.path

import io.ktor.resources.Resource

/**
 * Therapy-content authoring, review, and publication collection.
 *
 * The authenticated actor is not supplied through this resource. The route layer must resolve the actor from the
 * authentication principal.
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
     * Retrieves or mutates one therapy-session version.
     */
    @Resource("{therapySessionId}")
    data class ById(
        val therapySessionId: String,
        val parent: ManagedTherapyRoutesPath = ManagedTherapyRoutesPath(),
    )

    /**
     * Retrieves the highest persisted version in one therapy-session series.
     */
    @Resource("series/{seriesId}/latest")
    data class LatestVersion(
        val seriesId: String,
        val parent: ManagedTherapyRoutesPath = ManagedTherapyRoutesPath(),
    )

    /**
     * Manages modules owned by one therapy-session draft.
     */
    @Resource("{therapySessionId}/modules")
    data class Modules(
        val therapySessionId: String,
        val parent: ManagedTherapyRoutesPath = ManagedTherapyRoutesPath(),
    ) {

        /**
         * Replaces the complete ordering of the session's modules.
         */
        @Resource("reorder")
        data class Reorder(
            val parent: Modules,
        )

        /**
         * Mutates one module owned by the parent therapy session.
         */
        @Resource("{therapyModuleId}")
        data class ById(
            val therapyModuleId: String,
            val parent: Modules,
        )
    }
}
