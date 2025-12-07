package io.rewynd.api

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigUtil
import io.github.oshai.kotlinlogging.KotlinLogging

data class ApiSettings(val secureSession: Boolean) {
    companion object {
        private val log = KotlinLogging.logger { }
        fun fromConfig(config: Config = ConfigFactory.load()) =
            config.getConfig(
                ConfigUtil.joinPath("rewynd-api"),
            ).run {
                ApiSettings(
                    secureSession = getBoolean("secure-session")
                )
            }.also { log.info { "Loaded $it" } }
    }
}
