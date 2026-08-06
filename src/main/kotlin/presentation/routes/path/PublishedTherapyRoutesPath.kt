package com.simbiri.presentation.routes.path

import io.ktor.resources.Resource

/**
 * Public therapy-session catalogue.
 */
@Resource("/therapy-sessions")
data class PublishedTherapyRoutesPath(
    val goal: String? = null,
    val intensity: String? = null,
    val locale: String? = null,
) {

    /**
     * Retrieves one published therapy session.
     */
    @Resource("{therapySessionId}")
    data class ById(
        val therapySessionId: String,
        val parent: PublishedTherapyRoutesPath =
            PublishedTherapyRoutesPath(),
    )
    
    /**
     * Provides temporary delivery access to assets belonging to published content.
     */
    @Resource("{therapySessionId}/assets")
    data class Assets(
        val therapySessionId: String,
        val parent: PublishedTherapyRoutesPath = PublishedTherapyRoutesPath(),
    ) {

        @Resource("{therapyAssetId}/download")
        data class Download(
            val therapyAssetId: String,
            val parent: Assets,
        )
    }
}
