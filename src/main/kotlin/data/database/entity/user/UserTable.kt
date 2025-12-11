package com.simbiri.data.database.entity.user


import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

object UserTable : UUIDTable("users") {
    // Core identity
    val accountName = varchar("account_name", 50).uniqueIndex()
    val emailAddress = varchar("email_address", 255).uniqueIndex()

    // Profile
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val about = text("about").nullable()
    val tagline = varchar("tagline", 255).nullable()
    val profileUrl = varchar("profile_url", 512).nullable()
    val backgroundUrl = varchar("background_url", 512).nullable()

    // Demographics
    val birthDate = date("birth_date")

    // Type / role (DEV, REGULAR, POST_MODERATOR, EVENTS_MODERATOR, PSYCHOLOGIST, ADMIN_EXEC, etc.)
    // Store as an integer code; map to your domain enum in the mapper.
    val userType = integer("user_type")

    // Flags
    val emailOptIn = bool("email_opt_in").default(false)
    val isPrivate = bool("is_private").default(true)
    val isAnonymous = bool("is_anonymous").default(false)
    val isActive = bool("is_active").default(true)

    // Auditing
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
