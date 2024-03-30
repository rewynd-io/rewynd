package io.rewynd.common.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigUtil
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
                        if (split.isEmpty() || split.size > 2) {
                            CacheConfig.log.warn { "Invalid host:port combination: $it" }
                            null
                        } else {
                            val port = split.getOrNull(1)?.toIntOrNull() ?: 6379
                            RedisURI.create(split[0], port)
                        }
                    },
            )
        }
    } else {
        null
    }

fun CacheConfig.Companion.fromConfig(config: Config) =
    config.getConfig(
        ConfigUtil.joinPath("rewynd", "cache"),
    ).let {
        requireNotNull(
            sequenceOf(
                { CacheConfig.RedisConfig.fromConfig(it) },
                { CacheConfig.RedisClusterConfig.fromConfig(it) },
            ).mapNotNull { it() }.firstOrNull(),
        ) { "No cache configured" }
    }
