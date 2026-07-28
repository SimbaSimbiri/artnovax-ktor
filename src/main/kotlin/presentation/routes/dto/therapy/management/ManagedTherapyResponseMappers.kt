package com.simbiri.presentation.routes.dto.therapy.management

import com.simbiri.domain.model.therapy.TherapyAsset
import com.simbiri.domain.model.therapy.TherapyModule
import com.simbiri.domain.model.therapy.TherapySession

/**
 * Maps a persisted therapy session to its compact management response.
 */
fun TherapySession.toManagedSummaryResponseDto():
        ManagedTherapySessionSummaryResponseDto =
    ManagedTherapySessionSummaryResponseDto(
        id = requireTherapySessionId(),
        seriesId = requireTherapySessionSeriesId(),
        authorId = authorId.value.toString(),

        title = title,
        description = description,
        tagline = tagline,

        status = status.name,
        version = version,

        therapeuticPriority =
            therapeuticPriority.name,
        intensity = intensity.name,
        locale = locale,

        goalTags = sortedGoalNames(),
        contraindications =
            sortedContraindicationNames(),
        cultureTags = sortedCultureTags(),

        coverAsset =
            coverAsset?.toManagedResponseDto(),

        moduleCount = moduleCount,
        estimatedDurationSeconds =
            estimatedDurationSeconds,
        estimatedDurationMinutes =
            estimatedDurationMinutes,

        createdAt = requireCreatedAt(),
        updatedAt = requireUpdatedAt(),
        publishedAt = publishedAt?.toString(),
        archivedAt = archivedAt?.toString(),
    )

/**
 * Maps management collection results while retaining repository order.
 */
fun List<TherapySession>.toManagedSummaryResponseDtos():
        List<ManagedTherapySessionSummaryResponseDto> =
    map(TherapySession::toManagedSummaryResponseDto)

/**
 * Maps a persisted therapy session to its full management response.
 */
fun TherapySession.toManagedResponseDto():
        ManagedTherapySessionResponseDto =
    ManagedTherapySessionResponseDto(
        id = requireTherapySessionId(),
        seriesId = requireTherapySessionSeriesId(),
        authorId = authorId.value.toString(),

        title = title,
        description = description,
        tagline = tagline,

        status = status.name,
        version = version,

        therapeuticPriority =
            therapeuticPriority.name,
        intensity = intensity.name,
        locale = locale,

        goalTags = sortedGoalNames(),
        contraindications =
            sortedContraindicationNames(),
        cultureTags = sortedCultureTags(),

        coverAsset =
            coverAsset?.toManagedResponseDto(),

        modules =
            modules
                .sortedBy { module ->
                    module.orderIndex
                }
                .map(TherapyModule::toManagedResponseDto),

        moduleCount = moduleCount,
        estimatedDurationSeconds =
            estimatedDurationSeconds,
        estimatedDurationMinutes =
            estimatedDurationMinutes,

        createdAt = requireCreatedAt(),
        updatedAt = requireUpdatedAt(),
        publishedAt = publishedAt?.toString(),
        archivedAt = archivedAt?.toString(),
    )

private fun TherapySession.requireTherapySessionId(): String =
    requireNotNull(id) {
        "Persisted therapy session is missing its ID."
    }.value.toString()

private fun TherapySession.requireTherapySessionSeriesId(): String =
    requireNotNull(seriesId) {
        "Persisted therapy session is missing its series ID. " +
                "therapySessionId=${id?.value}."
    }.value.toString()

private fun TherapySession.requireCreatedAt(): String =
    requireNotNull(createdAt) {
        "Persisted therapy session is missing createdAt. " +
                "therapySessionId=${id?.value}."
    }.toString()

private fun TherapySession.requireUpdatedAt(): String =
    requireNotNull(updatedAt) {
        "Persisted therapy session is missing updatedAt. " +
                "therapySessionId=${id?.value}."
    }.toString()

private fun TherapySession.sortedGoalNames(): List<String> =
    goalTags
        .map { goal ->
            goal.name
        }
        .sorted()

private fun TherapySession.sortedContraindicationNames():
        List<String> =
    contraindications
        .map { contraindication ->
            contraindication.name
        }
        .sorted()

private fun TherapySession.sortedCultureTags(): List<String> =
    cultureTags
        .map(String::trim)
        .sortedBy(String::lowercase)

private fun TherapyModule.toManagedResponseDto():
        ManagedTherapyModuleResponseDto =
    ManagedTherapyModuleResponseDto(
        id =
            requireNotNull(id) {
                "Persisted therapy module is missing its ID."
            }.value.toString(),

        orderIndex = orderIndex,
        title = title,
        goal = goal,
        instructions = instructions,
        whyThisHelps = whyThisHelps,

        modality = modality.name,
        estimatedDurationSeconds =
            estimatedDurationSeconds,

        isSkippable = isSkippable,
        isRepeatable = isRepeatable,

        assets =
            assets
                .sortedWith(
                    compareBy<TherapyAsset>(
                        { asset -> asset.role.name },
                        { asset -> asset.storageKey },
                    )
                )
                .map(TherapyAsset::toManagedResponseDto),

        createdAt =
            requireNotNull(createdAt) {
                "Persisted therapy module is missing createdAt. " +
                        "therapyModuleId=${id?.value}."
            }.toString(),

        updatedAt =
            requireNotNull(updatedAt) {
                "Persisted therapy module is missing updatedAt. " +
                        "therapyModuleId=${id?.value}."
            }.toString(),
    )

/**
 * Management responses expose the durable storage key because authorized
 * authors and reviewers need to inspect the underlying asset metadata.
 */
private fun TherapyAsset.toManagedResponseDto():
        ManagedTherapyAssetResponseDto =
    ManagedTherapyAssetResponseDto(
        id =
            requireNotNull(id) {
                "Persisted therapy asset is missing its ID."
            }.value.toString(),

        role = role.name,
        mediaType = mediaType.name,

        storageKey = storageKey,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        sha256 = sha256,

        locale = locale,
        altText = altText,
        transcript = transcript,

        createdAt =
            requireNotNull(createdAt) {
                "Persisted therapy asset is missing createdAt. " +
                        "therapyAssetId=${id?.value}."
            }.toString(),

        updatedAt =
            requireNotNull(updatedAt) {
                "Persisted therapy asset is missing updatedAt. " +
                        "therapyAssetId=${id?.value}."
            }.toString(),
    )
