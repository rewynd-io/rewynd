package io.rewynd.common.config

import io.lettuce.core.RedisURI
import io.rewynd.common.KLog

sealed interface CacheConfig {
    data class RedisConfig(
        val hostname: String,
        val port: Int,
    ) : CacheConfig {
        companion object
    }

    data class RedisClusterConfig(
        val uris: List<RedisURI>,
    ) : CacheConfig {
        companion object
    }

    companion object : KLog()
}
