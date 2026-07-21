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
    /*
    * These values are absent for users constructed from create or update
    * requests. The persistence layer assigns them when the user is stored.
    *
    * Users loaded from the repository must contain both timestamps.
    */
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    /**
     * Delegates capability to UserType, the ssot for user role permissions
     */
    val canExposeSocialLinks: Boolean
        get() = type.canExposeSocialLinks

    val canAuthorTherapyContent: Boolean
        get() = type.canAuthorTherapyContent

    val canReviewTherapyContent: Boolean
        get() = type.canReviewTherapyContent

    val canPublishTherapyContent: Boolean
        get() = type.canPublishTherapyContent
}
