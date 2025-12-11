package com.simbiri.data.mapper.user

import com.simbiri.data.database.entity.user.UserEntity
import com.simbiri.data.database.entity.user.UserTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toUserEntity(): UserEntity =
    UserEntity(
        id = this[UserTable.id].value,
        accountName = this[UserTable.accountName],
        emailAddress = this[UserTable.emailAddress],
        firstName = this[UserTable.firstName],
        lastName = this[UserTable.lastName],
        birthDate = this[UserTable.birthDate],
        about = this[UserTable.about],
        tagline = this[UserTable.tagline],
        profileUrl = this[UserTable.profileUrl],
        backgroundUrl = this[UserTable.backgroundUrl],
        userTypeCode = this[UserTable.userType],
        emailOptIn = this[UserTable.emailOptIn],
        isPrivate = this[UserTable.isPrivate],
        isAnonymous = this[UserTable.isAnonymous],
        isActive = this[UserTable.isActive],
        createdAt = this[UserTable.createdAt],
        updatedAt = this[UserTable.updatedAt]
    )
