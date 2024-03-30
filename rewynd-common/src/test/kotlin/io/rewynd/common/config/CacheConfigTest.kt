package io.rewynd.common.config

import com.typesafe.config.ConfigFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

internal class CacheConfigTest : StringSpec({
    "Redis config should be parsed" {
        CacheConfig.fromConfig(
            ConfigFactory.load("redis-test"),
        ).run {
            shouldBeInstanceOf<CacheConfig.RedisConfig>()
            hostname shouldBe "localhost"
            port shouldBe 6379
        }
    }
})
