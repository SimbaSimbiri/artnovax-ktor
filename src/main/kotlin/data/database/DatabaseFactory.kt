package com.simbiri.data.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

object DatabaseFactory {

    fun create(): Database {
        val jdbcUrl = System.getenv("JDBC_URL")
            ?: error("JDBC_URL not set in environment")
        val dbUser = System.getenv("DB_USER")
            ?: error("DB_USER not set in environment")
        val dbPassword = System.getenv("DB_PASS")
            ?: error("DB_PASS not set in environment")

        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = dbUser
            this.password = dbPassword
            this.driverClassName = "org.postgresql.Driver"
            this.maximumPoolSize = 10
            this.isAutoCommit = false
            this.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(hikariConfig)

        val database = Database.connect(dataSource)
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ

        // simple migrations for now – later you can plug in Flyway/Liquibase
        transaction(database) {
            SchemaUtils.create(
                // UsersTable,
                // CommunitiesTable,
                // EventsTable,
                // ...
            )
        }

        return database
    }

    // Helper for suspending queries, like your Mongo coroutines
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}