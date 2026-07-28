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
}
