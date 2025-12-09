package com.simbiri.domain.model.common

import java.time.Instant
import java.util.UUID


@JvmInline
value class UserId(val value: UUID)

typealias Timestamp = Instant
