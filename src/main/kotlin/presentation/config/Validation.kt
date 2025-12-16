package com.simbiri.presentation.config

import com.simbiri.presentation.validator.validateCommunityMemberUpsert
import com.simbiri.presentation.validator.validateCommunityUpsert
import com.simbiri.presentation.validator.validateUserUpsert
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*

fun Application.configureValidation() {
    install(RequestValidation) {
        // we will add our incoming dto's request validations in separate files
        validateUserUpsert()
        validateCommunityUpsert()
        validateCommunityMemberUpsert()
    }
}