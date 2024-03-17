package io.rewynd.common.config

import com.typesafe.config.ConfigFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.property.forAll

internal class CacheConfigTest : StringSpec({
    "Redis config should be parsed" {
        forAll<CacheConfig.RedisConfig> {
            CacheConfig.fromConfig(
                ConfigFactory.parseMap(
                    mapOf(
                        "redis" to
                            mapOf(
                                "hostname" to it.hostname,
                                "port" to it.port,
                            ),
                    ),
                ),
            ) == it
        }
    }
})
