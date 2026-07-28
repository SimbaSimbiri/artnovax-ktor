package com.simbiri.presentation.routes.dto.therapy.public

import com.simbiri.domain.model.therapy.TherapyAsset
import com.simbiri.domain.model.therapy.TherapyContentStatus
import com.simbiri.domain.model.therapy.TherapyModule
import com.simbiri.domain.model.therapy.TherapySession

/**
 * Maps a persisted published session to its compact catalogue response.
 */
fun TherapySession.toPublishedSummaryResponseDto(): PublishedTherapySessionSummaryResponseDto {
    validatePublishedResponseState()

    return PublishedTherapySessionSummaryResponseDto(
        id = requireTherapySessionId(),
        seriesId = requireTherapySessionSeriesId(),

        title = title,
        description = description,
        tagline = tagline,

        intensity = intensity.name,
        locale = locale,
        version = version,
        therapeuticPriority = therapeuticPriority.name,

        goalTags = sortedGoalNames(),
        contraindications = sortedContraindicationNames(),
        cultureTags = sortedCultureTags(),

        coverAsset = coverAsset?.toPublishedResponseDto(),

        moduleCount = moduleCount,
        estimatedDurationSeconds = estimatedDurationSeconds,
        estimatedDurationMinutes = estimatedDurationMinutes,

        publishedAt = requireNotNull(publishedAt) {
            "Published therapy session is missing publishedAt. " + "therapySessionId=${id?.value}."
        }.toString(),
    )
}

/**
 * Maps public catalogue results while preserving repository ordering.
 */
fun List<TherapySession>.toPublishedSummaryResponseDtos(): List<PublishedTherapySessionSummaryResponseDto> =
    map(TherapySession::toPublishedSummaryResponseDto)

/**
 * Maps a persisted published session to its complete public response.
 */
fun TherapySession.toPublishedResponseDto(): PublishedTherapySessionResponseDto {
    validatePublishedResponseState()

    return PublishedTherapySessionResponseDto(
        id = requireTherapySessionId(),
        seriesId = requireTherapySessionSeriesId(),

        title = title,
        description = description,
        tagline = tagline,

        intensity = intensity.name,
        locale = locale,
        version = version,
        therapeuticPriority = therapeuticPriority.name,

        goalTags = sortedGoalNames(),
        contraindications = sortedContraindicationNames(),
        cultureTags = sortedCultureTags(),

        coverAsset = coverAsset?.toPublishedResponseDto(),

        modules = modules.sortedBy { module ->
                module.orderIndex
            }.map(TherapyModule::toPublishedResponseDto),

        moduleCount = moduleCount,
        estimatedDurationSeconds = estimatedDurationSeconds,
        estimatedDurationMinutes = estimatedDurationMinutes,

        publishedAt = requireNotNull(publishedAt) {
            "Published therapy session is missing publishedAt. " + "therapySessionId=${id?.value}."
        }.toString(),
    )
}

/**
 * Prevents presentation code from accidentally exposing a private
 * therapy-content version through a public response mapper.
 */
private fun TherapySession.validatePublishedResponseState() {
    check(status == TherapyContentStatus.PUBLISHED) {
        "Only PUBLISHED therapy content may be mapped to a public response. therapySessionId=${id?.value}, " +
                "status=$status."
    }
}

private fun TherapySession.requireTherapySessionId(): String = requireNotNull(id) {
    "Persisted therapy session is missing its ID."
}.value.toString()

private fun TherapySession.requireTherapySessionSeriesId(): String = requireNotNull(seriesId) {
    "Persisted therapy session is missing its series ID. therapySessionId=${id?.value}."
}.value.toString()

private fun TherapySession.sortedGoalNames(): List<String> = goalTags.map { goal ->
        goal.name
    }.sorted()

private fun TherapySession.sortedContraindicationNames(): List<String> = contraindications.map { contraindication ->
        contraindication.name
    }.sorted()

private fun TherapySession.sortedCultureTags(): List<String> = cultureTags.map(String::trim)
    .sortedBy(String::lowercase)

private fun TherapyModule.toPublishedResponseDto(): PublishedTherapyModuleResponseDto =
    PublishedTherapyModuleResponseDto(
        id = requireNotNull(id) {
            "Persisted therapy module is missing its ID."
        }.value.toString(),

        orderIndex = orderIndex,
        title = title,
        goal = goal,
        instructions = instructions,
        whyThisHelps = whyThisHelps,
        modality = modality.name,
        estimatedDurationSeconds = estimatedDurationSeconds,
        isSkippable = isSkippable,
        isRepeatable = isRepeatable,

        assets = assets.sortedWith(
                compareBy<TherapyAsset>(
                    { asset -> asset.role.name },
                    { asset -> asset.mediaType.name },
                    { asset -> asset.storageKey },
                )
            ).map(TherapyAsset::toPublishedResponseDto),
    )

/**
 * storageKey and persistence timestamps are deliberately excluded from
 * the public representation.
 */
private fun TherapyAsset.toPublishedResponseDto(): PublishedTherapyAssetResponseDto = PublishedTherapyAssetResponseDto(
    id = requireNotNull(id) {
        "Persisted therapy asset is missing its ID."
    }.value.toString(),

    role = role.name,
    mediaType = mediaType.name,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    sha256 = sha256,
    locale = locale,
    altText = altText,
    transcript = transcript,
)
