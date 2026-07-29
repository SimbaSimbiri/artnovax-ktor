package com.simbiri.domain.model.user

import com.simbiri.domain.model.social.SocialLink
import java.time.LocalDate

/**
 * Mutable profile information supplied by the authenticated user.
 *
 * Authentication identity, authorization role, active status, account
 * name, email address, and persistence metadata are absent.
 */
data class CurrentUserProfileUpdate(
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,

    val about: String?,
    val tagline: String?,

    val profileUrl: String?,
    val backgroundUrl: String?,

    val emailOptIn: Boolean,
    val isPrivate: Boolean,
    val isAnonymous: Boolean,

    val socialLinks: List<SocialLink>,
)
