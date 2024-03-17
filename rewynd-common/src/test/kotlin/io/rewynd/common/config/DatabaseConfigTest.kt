package io.rewynd.common.config

import com.typesafe.config.ConfigFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.property.forAll

internal class DatabaseConfigTest : StringSpec({
    "Postgres config should be parsed" {
        forAll<DatabaseConfig.PostgresConfig> {
            DatabaseConfig.fromConfig(
                ConfigFactory.parseMap(
                    mapOf(
                        "postgres" to
                            mapOf(
                                "hostname" to it.hostname,
                                "username" to it.username,
                                "password" to it.password,
                                "port" to it.port,
                                "database" to it.database,
                            ),
                    ),
                ),
            ) == it
        }
    }
})
