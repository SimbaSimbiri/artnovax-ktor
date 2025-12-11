package com.simbiri.domain.model.user

import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.social.SocialLink
import java.time.LocalDate

data class User(
    val id: UserId? = null,
    val accountName: String,
    val emailAddress: String,
    val birthDate: LocalDate,
    val about: String?,
    val tagline: String?,
    val firstName: String,
    val lastName: String,
    val profileUrl: String?,
    val backgroundUrl: String?,
    val type: UserType,
    val emailOptIn: Boolean,
    val isPrivate: Boolean,
    val isAnonymous: Boolean,
    val isActive: Boolean,
    val socialLinks: List<SocialLink>,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
){
    val canExposeSocialLinks: Boolean
        get() = type == UserType.PSYCHOLOGIST || type == UserType.ADMIN_EXEC || type == UserType.DEV
}
