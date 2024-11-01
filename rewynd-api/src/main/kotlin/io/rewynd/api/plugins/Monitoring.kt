package io.rewynd.api.plugins

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callid.generate
import io.ktor.server.plugins.calllogging.CallLogging
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.UUID

private val log by lazy { KotlinLogging.logger { } }

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        logger = LoggerFactory.getLogger(log.name)
        callIdMdc("call-id")
    }
    install(CallId) {
        header(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }
}
