package com.simbiri.domain.policy.user

import com.simbiri.domain.model.user.User

/**
 * Controls which parts of a User may be exposed through public profile
 * endpoints.
 */
object UserProfileVisibilityPolicy {

    /**
     * Only active, non-private users may appear in public profile reads.
     */
    fun isPubliclyVisible(
        user: User,
    ): Boolean = user.isActive && !user.isPrivate

    /**
     * Anonymous users retain a public account identity but do not expose
     * their legal or personal name.
     */
    fun canExposeRealName(
        user: User,
    ): Boolean = isPubliclyVisible(user) && !user.isAnonymous

    /**
     * Social links require both an eligible user type and a non-anonymous
     * public profile.
     */
    fun canExposeSocialLinks(
        user: User,
    ): Boolean = isPubliclyVisible(user) && !user.isAnonymous && user.canExposeSocialLinks
}
