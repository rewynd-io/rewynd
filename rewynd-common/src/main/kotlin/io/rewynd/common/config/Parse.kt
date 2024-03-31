package io.rewynd.common.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.RedisURI

fun CacheConfig.RedisConfig.Companion.fromConfig(config: Config) =
    if (
        config.hasPath("redis") &&
        config.hasPath("redis.hostname") &&
        config.hasPath("redis.port")
    ) {
        with(config.getConfig("redis")) {
            CacheConfig.RedisConfig(
                hostname = getString("hostname"),
                port = getInt("port"),
            )
        }
    } else {
        null
    }

val CacheConfig.RedisConfig.uri: RedisURI
    get() = RedisURI.create(hostname, port)

fun CacheConfig.RedisClusterConfig.Companion.fromConfig(config: Config) =
    if (
        config.hasPath("redis-cluster") &&
        config.hasPath("redis-cluster.hosts")
    ) {
        with(config) {
            CacheConfig.RedisClusterConfig(
                uris =
                getString("hosts").split(",").mapNotNull {
                    val split = it.split(":")
                    check(split.size == 2) {
                        "Invalid host:port combination: $it"
                    }
                    val port = split[1].toInt()
                    RedisURI.create(split[0], port)
                },
            )
        }
    } else {
        null
    }

fun CacheConfig.MemoryConfig.fromConfig(config: Config) =
    if (
        config.hasPath("memory")
    ) {
        CacheConfig.MemoryConfig
    } else {
        null
    }

private val log by lazy { KotlinLogging.logger { } }
fun CacheConfig.Companion.fromConfig(config: Config = ConfigFactory.load()) =
    config.getConfig(
        ConfigUtil.joinPath("rewynd", "cache"),
    ).let {
        requireNotNull(
            sequenceOf(
                { CacheConfig.RedisConfig.fromConfig(it) },
                { CacheConfig.RedisClusterConfig.fromConfig(it) },
                { CacheConfig.MemoryConfig.fromConfig(it) },
            ).mapNotNull { it() }.firstOrNull(),
        ) { "No cache configured" }
    }.also { log.info { "Loaded $it" } }
