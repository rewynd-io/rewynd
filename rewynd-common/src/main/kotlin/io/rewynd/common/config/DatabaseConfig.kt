package io.rewynd.common.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigUtil
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import javax.sql.DataSource
import kotlin.io.path.absolutePathString
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

    data class SqliteConfig(
        val dbFile: Path?,
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
        config.leakDetectionThreshold = 5.minutes.inWholeMilliseconds
        return HikariDataSource(config)
    }

val DatabaseConfig.SqliteConfig.datasource: DataSource
    get() {
        val config = HikariConfig()
        config.jdbcUrl = url
        config.leakDetectionThreshold = 5.minutes.inWholeMilliseconds
        return HikariDataSource(config)
    }

val DatabaseConfig.url
    get() =
        when (this) {
            is DatabaseConfig.PostgresConfig -> "jdbc:postgresql://$hostname:$port/$database"
            is DatabaseConfig.SqliteConfig ->
                "jdbc:sqlite:${
                    dbFile?.absolutePathString() ?: "file:test?mode=memory&cache=shared"
                }"
        }

val DatabaseConfig.driver
    get() =
        when (this) {
            is DatabaseConfig.PostgresConfig -> "org.postgresql.Driver"
            is DatabaseConfig.SqliteConfig -> "org.sqlite.JDBC"
        }

fun DatabaseConfig.SqliteConfig.Companion.fromConfig(config: Config) =
    if (config.hasPath("sqlite")) {
        with(config.getConfig("sqlite")) {
            DatabaseConfig.SqliteConfig(
                dbFile =
                if (hasPath("db-file")) {
                    Path.of(getString("db-file"))
                } else {
                    null
                },
            )
        }
    } else {
        null
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

private val log by lazy { KotlinLogging.logger { } }

fun DatabaseConfig.Companion.fromConfig(config: Config = ConfigFactory.load()) =
    config.getConfig(
        ConfigUtil.joinPath("rewynd", "database"),
    ).let {
        requireNotNull(
            sequenceOf(
                { DatabaseConfig.PostgresConfig.fromConfig(it) },
                { DatabaseConfig.SqliteConfig.fromConfig(it) },
            ).mapNotNull { it() }.firstOrNull(),
        ) { "No database configured" }
    }.also { log.info { "Loaded $it" } }
