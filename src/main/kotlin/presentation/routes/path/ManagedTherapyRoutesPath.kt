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

    /**
     * Groups commands that transition authored therapy content through its controlled lifecycle.
     */
    @Resource("{therapySessionId}/lifecycle")
    data class Lifecycle(
        val therapySessionId: String,
        val parent: ManagedTherapyRoutesPath = ManagedTherapyRoutesPath(),
    ) {

        /**
         * Moves complete draft content into clinical or administrative review.
         */
        @Resource("submit-for-review")
        data class SubmitForReview(
            val parent: Lifecycle,
        )

        /**
         * Returns reviewed content to its editable draft state.
         */
        @Resource("return-to-draft")
        data class ReturnToDraft(
            val parent: Lifecycle,
        )

        /**
         * Makes reviewed therapy content available through the public catalogue.
         */
        @Resource("publish")
        data class Publish(
            val parent: Lifecycle,
        )

        /**
         * Removes published content from discovery while retaining its historical version.
         */
        @Resource("archive")
        data class Archive(
            val parent: Lifecycle,
        )
    }

    /**
     * Requests temporary upload access for a therapy-session or module asset.
     */
    @Resource("{therapySessionId}/asset-uploads")
    data class AssetUploads(
        val therapySessionId: String,
        val parent: ManagedTherapyRoutesPath = ManagedTherapyRoutesPath(),
    )
}
