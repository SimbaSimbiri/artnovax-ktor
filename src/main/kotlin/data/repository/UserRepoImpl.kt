package com.simbiri.data.repository

import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.social.SocialLink
import com.simbiri.domain.model.social.SocialPlatform
import com.simbiri.domain.model.user.User
import com.simbiri.domain.model.user.UserType
import com.simbiri.domain.repository.UserRepository
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import kotlinx.datetime.LocalDate
import java.time.Instant
import java.util.*

class UserRepoImpl(): UserRepository {
    val userList = dummyUsers()

    override suspend fun getAllUsers(userType: Int?): ResultType<List<User>, DataError> {
        val listRtn = if(userType == null) {userList} else{userList.filter { it.type.code == userType }}

        return if (listRtn.isNotEmpty()) ResultType.Success(listRtn)
        else ResultType.Failure(DataError.NotFound)
    }

    override suspend fun getUserById(userId: String?): ResultType<User, DataError> {
        val user= userList.find { it.id.value.toString() == userId }

        return if(user!=null){
            ResultType.Success(user)
        } else{
            ResultType.Failure(DataError.NotFound)
        }
    }

    override suspend fun upsertUser(userRec: User): ResultType<Unit, DataError> {
        // convert to Entities before inserting or updating
        return ResultType.Success(Unit)
    }

    override suspend fun deleteUserById(userId: String?): ResultType<Unit, DataError> {
        val deleted = userList.removeIf { it -> it.id.value.toString() == userId }

        return if (deleted){
            ResultType.Success(Unit)
        } else {
            ResultType.Failure(DataError.NotFound)
        }
    }

    override suspend fun insertUsersInBulk(users: List<User>): ResultType<Unit, DataError> {

        // convert to Entities before inserting
        return ResultType.Success(Unit)
    }
}

private val instagramPlatform = SocialPlatform(
    id = 1,
    name = "instagram",
    baseUrl = "https://instagram.com"
)

private val linkedinPlatform = SocialPlatform(
    id = 2,
    name = "linkedin",
    baseUrl = "https://www.linkedin.com/in"
)

fun dummyUsers():MutableList<User> {
    val now: Instant = Timestamp.now()

    val devSocial = SocialLink(
        platform = instagramPlatform,
        username = "dev_simbiri",
        completeUrl = "https://instagram.com/dev_simbiri"
    )

    val psychSocial = SocialLink(
        platform = instagramPlatform,
        username = "artnovax_psych",
        completeUrl = "https://instagram.com/artnovax_psych"
    )

    val adminSocial = SocialLink(
        platform = linkedinPlatform,
        username = "artnovax-admin",
        completeUrl = "https://www.linkedin.com/in/artnovax-admin"
    )

    val eventsModSocial = SocialLink(
        platform = instagramPlatform,
        username = "events_novax",
        completeUrl = "https://instagram.com/events_novax"
    )

    // DEV (your existing example)
    val userDev = User(
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
        isAnonymous = false,
        isActive = true,
        socialLinks = listOf(devSocial),
        createdAt = now,
        updatedAt = now
    )

    // REGULAR user (no socials, anonymous allowed)
    val userRegular = User(
        id = UserId(UUID.randomUUID()),
        accountName = "quietArtist",
        emailAddress = "user@example.com",
        birthDate = LocalDate.parse("2005-06-10"),
        about = "Just here to try some art therapy.",
        tagline = "Taking it one brush stroke at a time.",
        firstName = "Alex",
        lastName = "Doe",
        profileUrl = null,
        backgroundUrl = null,
        type = UserType.REGULAR,
        emailOptIn = false,
        isPrivate = true,
        isAnonymous = true,           // main therapy user stays anonymous
        isActive = true,
        socialLinks = emptyList(),     // business rule: no socials for REGULAR
        createdAt = now,
        updatedAt = now
    )

    // POST_MODERATOR
    val userPostModerator = User(
        id = UserId(UUID.randomUUID()),
        accountName = "postGuardian",
        emailAddress = "posts@artnovax.com",
        birthDate = LocalDate.parse("1998-03-21"),
        about = "Helps keep the feed safe and supportive.",
        tagline = "Here to make sure everyone feels safe sharing.",
        firstName = "Maya",
        lastName = "Ngugi",
        profileUrl = null,
        backgroundUrl = null,
        type = UserType.POST_MODERATOR,
        emailOptIn = true,
        isPrivate = false,
        isAnonymous = false,
        isActive = true,
        socialLinks = emptyList(),     // could add socials later if you want
        createdAt = now,
        updatedAt = now
    )

    // EVENTS_MODERATOR
    val userEventsModerator = User(
        id = UserId(UUID.randomUUID()),
        accountName = "eventsHost",
        emailAddress = "events@artnovax.com",
        birthDate = LocalDate.parse("1995-11-02"),
        about = "Organizes community and group therapy events.",
        tagline = "See you at the next breathing circle.",
        firstName = "Sam",
        lastName = "Okafor",
        profileUrl = null,
        backgroundUrl = null,
        type = UserType.EVENTS_MODERATOR,
        emailOptIn = true,
        isPrivate = false,
        isAnonymous = false,
        isActive = true,
        socialLinks = listOf(eventsModSocial),
        createdAt = now,
        updatedAt = now
    )

    // PSYCHOLOGIST
    val userPsychologist = User(
        id = UserId(UUID.randomUUID()),
        accountName = "drKariuki",
        emailAddress = "psych1@artnovax.com",
        birthDate = LocalDate.parse("1986-09-15"),
        about = "Clinical psychologist focusing on art-based interventions.",
        tagline = "Let’s turn emotions into color.",
        firstName = "Lena",
        lastName = "Kariuki",
        profileUrl = null,
        backgroundUrl = null,
        type = UserType.PSYCHOLOGIST,
        emailOptIn = true,
        isPrivate = false,
        isAnonymous = false,
        isActive = true,
        socialLinks = listOf(psychSocial),
        createdAt = now,
        updatedAt = now
    )

    // ADMIN_EXEC
    val userAdminExec = User(
        id = UserId(UUID.randomUUID()),
        accountName = "artnovaxAdmin",
        emailAddress = "admin@artnovax.com",
        birthDate = LocalDate.parse("1982-01-30"),
        about = "Oversees platform safety, ethics, and roadmap.",
        tagline = "Building safer spaces for creative healing.",
        firstName = "Noor",
        lastName = "Hassan",
        profileUrl = null,
        backgroundUrl = null,
        type = UserType.ADMIN_EXEC,
        emailOptIn = true,
        isPrivate = false,
        isAnonymous = false,
        isActive = true,
        socialLinks = listOf(adminSocial),
        createdAt = now,
        updatedAt = now
    )

    return mutableListOf(
        userDev,
        userRegular,
        userPostModerator,
        userEventsModerator,
        userPsychologist,
        userAdminExec
    )
}