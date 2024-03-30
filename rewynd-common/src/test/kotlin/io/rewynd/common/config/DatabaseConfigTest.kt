package io.rewynd.common.config

import com.typesafe.config.ConfigFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

internal class DatabaseConfigTest : StringSpec({
    "Postgres config should be parsed" {
        DatabaseConfig.fromConfig(
            ConfigFactory.load("postgres-test"),
        ).run {
            shouldBeInstanceOf<DatabaseConfig.PostgresConfig>()
            database shouldBe "rewynd"
            password shouldBe "password"
            username shouldBe "postgres"
            hostname shouldBe "localhost"
            port shouldBe 5432
        }
    }
})
