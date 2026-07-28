package com.simbiri.domain.policy.user

import java.util.Locale

object EmailAddressNormalizer {

    fun normalize(
        emailAddress: String,
    ): String =
        emailAddress
            .trim()
            .lowercase(Locale.ROOT)
}
