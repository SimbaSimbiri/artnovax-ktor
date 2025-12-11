package com.simbiri.domain.model.common

import java.time.Instant
import java.util.*


@JvmInline
value class UserId(val value: UUID)

@JvmInline
value class CommunityId(val value: UUID)

@JvmInline
value class SessionId(val value: UUID)

@JvmInline
value class ModuleId(val value: UUID)

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


typealias Timestamp = Instant
