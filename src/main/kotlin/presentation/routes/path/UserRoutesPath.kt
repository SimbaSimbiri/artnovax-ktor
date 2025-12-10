package com.simbiri.presentation.routes.path

import io.ktor.resources.Resource

@Resource("/users")
class UserRoutesPath(val userType: String? = null){

    @Resource("{userId}")
    data class ById(
        val parent: UserRoutesPath = UserRoutesPath(),
        val userId: String
    )
}
