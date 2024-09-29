package io.rewynd.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.Serializable

@Serializable
data class UserSession(val id: String, val username: String) {
    companion object {

        suspend fun PipelineContext<Unit, ApplicationCall>.withUsername(block: suspend String.() -> Unit) {
            val username = call.sessions.get<UserSession>()?.username
            if (username != null) {
                block(username)
            } else {
                call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}
