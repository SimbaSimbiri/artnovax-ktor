package com.simbiri.domain.model.social

object SocialPlatformRegistry {
    val INSTAGRAM = SocialPlatform(
        id = 1,
        name = "instagram",
        baseUrl = "https://instagram.com/"
    )

    val LINKEDIN = SocialPlatform(
        id = 2,
        name = "linkedin",
        baseUrl = "https://www.linkedin.com/in/"
    )

    val FACEBOOK = SocialPlatform(
        id = 3,
        name = "facebook",
        baseUrl = "https://www.facebook.com/"
    )

    val TIKTOK = SocialPlatform(
        id = 4,
        name = "tiktok",
        baseUrl = "https://www.tiktok.com/@"
    )

    val X = SocialPlatform(
        id = 5,
        name = "x",
        baseUrl = "https://x.com/"
    )

    val byId: Map<Int, SocialPlatform> = listOf(
        INSTAGRAM,
        LINKEDIN,
        FACEBOOK,
        TIKTOK,
        X
    ).associateBy { it.id }
}
