package io.rewynd.api

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.rewynd.common.config.CacheConfig
import io.rewynd.common.config.DatabaseConfig
import io.rewynd.common.config.fromConfig

data class ServerConfig(val database: DatabaseConfig, val cache: CacheConfig) {
    companion object {
        fun fromConfig(config: Config = ConfigFactory.load()) =
            ServerConfig(
                database = DatabaseConfig.fromConfig(config),
                cache = CacheConfig.fromConfig(config),
            )
    }
}
