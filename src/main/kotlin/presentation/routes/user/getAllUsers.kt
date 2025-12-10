package com.simbiri.presentation.routes.user

import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.social.SocialLink
import com.simbiri.domain.model.social.SocialPlatform
import com.simbiri.domain.model.user.User
import com.simbiri.domain.model.user.UserType
import com.simbiri.presentation.routes.dto.user.toResponseDto
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.datetime.LocalDate
import java.time.Instant
import java.util.UUID

fun Route.getAllUsers() {
    get("/users") {
        val now: Instant = Timestamp.now()

        val dummyPlatform = SocialPlatform(
            id = 1,
            name = "instagram",
            baseUrl = "https://instagram.com"
        )

        val dummySocial = SocialLink(
            platform = dummyPlatform,
            username = "dev_simbiri",
            completeUrl = "${dummyPlatform.baseUrl}/dev_simbiri"
        )

        val userSimbiri = User(
            id = UserId(UUID.randomUUID()),
            accountName = "devSimbiri",
            emailAddress = "simbiri@uchicago.edu",
            birthDate = LocalDate.parse("2002-12-14"),
            about = "Lead Software Engineer for ArtNovax.",
            tagline = "Art is therapy.",
            firstName = "Ray",
            lastName = "Simbiri",
            profileUrl = null,
            backgroundUrl = null,
            type = UserType.DEV,
            emailOptIn = true,
            isPrivate = false,
            isAnonymous = false,          // not anonymous, so socials can show
            isActive = true,
            socialLinks = listOf(dummySocial),
            createdAt = now,
            updatedAt = now
        )

        val dtoList = listOf(userSimbiri.toResponseDto())

        call.respond(dtoList)
    }
}