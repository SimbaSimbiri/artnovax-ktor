package com.simbiri.data.database.utils

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun <T> Database.dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO, this) {
        block()
    }