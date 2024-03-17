package io.rewynd.api.plugins

import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callloging.CallLogging
import mu.KotlinLogging
import org.slf4j.event.Level

private val log = KotlinLogging.logger { }

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        logger = log.underlyingLogger
        callIdMdc("call-id")
    }
    install(CallId) {
        header(HttpHeaders.XRequestId)
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }
}
