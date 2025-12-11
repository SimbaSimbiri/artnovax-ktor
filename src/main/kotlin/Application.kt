package com.simbiri

import com.simbiri.presentation.config.configureLogging
import com.simbiri.presentation.config.configureRouting
import com.simbiri.presentation.config.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureLogging()
    configureSerialization()
    configureRouting()
}
