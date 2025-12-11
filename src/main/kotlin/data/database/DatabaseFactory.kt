package com.simbiri.data.database

import com.simbiri.data.database.entity.social.SocialLinkTable
import com.simbiri.data.database.entity.social.SocialPlatformTable
import com.simbiri.data.database.entity.user.UserTable
import com.simbiri.data.database.utils.seedSocialPlatforms
import com.simbiri.data.util.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

object DatabaseFactory {

    fun create(): Database {
        val jdbcUrl = System.getenv(JDBC_URL)
            ?: error("JDBC_URL not set in environment")
        val dbUser = System.getenv(DB_USER)
            ?: error("DB_USER not set in environment")
        val dbPassword = System.getenv(DB_PASS)
            ?: error("DB_PASS not set in environment")

        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = dbUser
            this.password = dbPassword
            this.driverClassName = HIKARI_DRIVER_CLASS
            this.maximumPoolSize = 10
            this.isAutoCommit = false
            this.transactionIsolation = HIKARI_TRANS_ISOLATION
            validate()
        }

        val dataSource = HikariDataSource(hikariConfig)

        val database = Database.connect(dataSource)
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ

        transaction(database) {
            SchemaUtils.create(
                UserTable,
                SocialPlatformTable,
                SocialLinkTable
            )
        }

        seedSocialPlatforms()

        return database
    }

}
