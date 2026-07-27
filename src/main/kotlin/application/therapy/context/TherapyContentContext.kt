package com.simbiri.application.therapy.context

import com.simbiri.domain.model.therapy.TherapySession
import com.simbiri.domain.model.user.User

data class TherapyContentContext(
    val actor: User,
    val session: TherapySession,
)
