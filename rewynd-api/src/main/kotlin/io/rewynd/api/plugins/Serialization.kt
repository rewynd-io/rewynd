package io.rewynd.api.plugins

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.rewynd.common.JSON

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(JSON)
    }
}
