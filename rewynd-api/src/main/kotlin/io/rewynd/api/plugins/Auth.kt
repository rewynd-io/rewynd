package io.rewynd.api.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.rewynd.api.UserSession
import io.rewynd.common.database.Database
import mu.KotlinLogging

private val log = KotlinLogging.logger { }

fun mkAuthNPlugin() =
    createRouteScopedPlugin(name = "AuthN") {
        onCall {
            // TODO rework routing plugins instead of allow-listing specific paths
            if (it.request.path() != "/api/auth/login" && it.sessions.get<UserSession>() == null) {
                it.respond(HttpStatusCode.Forbidden)
            }
        }
    }

fun mkAdminAuthZPlugin(db: Database) =
    createRouteScopedPlugin(name = "AuthN") {
        onCall {
            val isAdmin =
                it.sessions.get<UserSession>()?.username?.let { user -> db.getUser(user)?.user?.permissions?.isAdmin }
                    ?: false
            if (!isAdmin) {
                it.respond(HttpStatusCode.Forbidden)
            }
        }
    }
