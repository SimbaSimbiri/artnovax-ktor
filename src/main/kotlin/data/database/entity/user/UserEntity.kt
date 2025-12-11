package com.simbiri.data.database.entity.user

import java.time.Instant
import java.time.LocalDate
import java.util.*

data class UserEntity(
    val id: UUID,
    val accountName: String,
    val emailAddress: String,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val about: String?,
    val tagline: String?,
    val profileUrl: String?,
    val backgroundUrl: String?,
    val userTypeCode: Int,
    val emailOptIn: Boolean,
    val isPrivate: Boolean,
    val isAnonymous: Boolean,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
