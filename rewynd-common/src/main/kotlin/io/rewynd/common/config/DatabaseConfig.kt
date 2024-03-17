package io.rewynd.common.config

import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import kotlin.time.Duration.Companion.minutes

sealed interface DatabaseConfig {
    data class PostgresConfig(
        val hostname: String,
        val username: String,
        val password: String,
        val port: Int,
        val database: String,
    ) : DatabaseConfig {
        companion object
    }

    companion object
}

val DatabaseConfig.PostgresConfig.datasource: DataSource
    get() {
        val config = HikariConfig()
        config.jdbcUrl = url
        config.username = username
        config.password = password
        // config.keepaliveTime = 5.minutes.inWholeMilliseconds
        config.leakDetectionThreshold = 5.minutes.inWholeMilliseconds
        return HikariDataSource(config)
    }

val DatabaseConfig.url
    get() =
        when (this) {
            is DatabaseConfig.PostgresConfig -> "jdbc:postgresql://$hostname:$port/$database"
        }

val DatabaseConfig.driver
    get() =
        when (this) {
            is DatabaseConfig.PostgresConfig -> "org.postgresql.Driver"
        }

fun DatabaseConfig.PostgresConfig.Companion.fromConfig(config: Config) =
    if (
        config.hasPath("postgres") &&
        config.hasPath("postgres.hostname") &&
        config.hasPath("postgres.username") &&
        config.hasPath("postgres.password") &&
        config.hasPath("postgres.port") &&
        config.hasPath("postgres.database")
    ) {
        with(config.getConfig("postgres")) {
            DatabaseConfig.PostgresConfig(
                hostname = getString("hostname"),
                username = getString("username"),
                password = getString("password"),
                port = getInt("port"),
                database = getString("database"),
            )
        }
    } else {
        null
    }

fun DatabaseConfig.Companion.fromConfig(config: Config) =
    requireNotNull(DatabaseConfig.PostgresConfig.fromConfig(config)) { "No database configured" }
