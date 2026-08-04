package com.simbiri.domain.model.common

import java.time.Instant
import java.util.*


@JvmInline
value class UserId(val value: UUID)

@JvmInline
value class CommunityId(val value: UUID)

@JvmInline
value class TherapySessionId(val value: UUID)

/**
 * An identifier shared by every version of the same authored
 * therapy session. e.g:
 *
 * seriesId = A, version = 1
 * seriesId = A, version = 2
 */
@JvmInline
value class TherapySessionSeriesId(
    val value: UUID,
)

@JvmInline
value class TherapyModuleId(val value: UUID)

@JvmInline
value class TherapyAssetId(val value: UUID)

@JvmInline
value class TherapySessionRunId(val value: UUID)

@JvmInline
value class TherapyModuleProgressId(val value: UUID)

@JvmInline
value class ArtworkId(val value: UUID)

@JvmInline
value class EventId(val value: UUID)

@JvmInline
value class PostId(val value: UUID)

@JvmInline
value class EmotionSnapshotId(val value: UUID)

@JvmInline
value class ConversationId(val value: UUID)

@JvmInline
value class MessageId(val value: UUID)

@JvmInline
value class RefreshSessionId( val value: UUID,)

@JvmInline
value class RefreshTokenFamilyId( val value: UUID,)

typealias Timestamp = Instant
