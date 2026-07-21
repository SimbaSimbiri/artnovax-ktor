package com.simbiri.domain.model.therapy

/**
 * Lifecycle state of authored therapy content.
 */
enum class TherapyContentStatus {
    /**
     * Editable content that is not visible to regular users.
     */
    DRAFT,

    /**
     * Submitted for clinical or administrative review.
     */
    IN_REVIEW,

    /**
     * Approved content available to users.
     */
    PUBLISHED,

    /**
     * Removed from discovery but retained for historical session runs.
     */
    ARCHIVED,
}
