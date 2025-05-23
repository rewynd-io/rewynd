package io.rewynd.api.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.generateSessionId
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.rewynd.api.UserSession
import io.rewynd.common.database.Database
import io.rewynd.common.decoder
import io.rewynd.common.hashPassword
import io.rewynd.model.LoginRequest
import java.security.MessageDigest

fun Route.authRoutes(db: Database) {
    route("/auth") {
        get("/verify") {
            val session = call.sessions.get<UserSession>()
            val user = session?.let { db.getUser(it.username) }
            if (user != null) {
                call.response.status(HttpStatusCode.OK)
                call.respond(user.user)
            } else {
                call.sessions.clear<UserSession>()
                call.response.status(
                    HttpStatusCode.Forbidden,
                )
            }
        }
        post("/logout") {
            call.sessions.clear<UserSession>()
            call.respond(HttpStatusCode.OK)
        }
        post("/login") {
            val request = call.receive<LoginRequest>()
            val username = request.username
            val password = request.password
            val response = if (username != null && password != null) {
                val user = db.getUser(username)
                if (user != null) {
                    val hashedPass = hashPassword(password, user.salt)
                    if (MessageDigest.isEqual(decoder.decode(hashedPass), decoder.decode(user.hashedPass))) {
                        call.sessions.set<UserSession>(UserSession(generateSessionId(), username))
                        HttpStatusCode.OK
                    } else {
                        HttpStatusCode.Forbidden
                    }
                } else {
                    HttpStatusCode.Forbidden
                }
            } else {
                HttpStatusCode.BadRequest
            }
            if (response != HttpStatusCode.OK) {
                call.sessions.clear<UserSession>()
            }
            call.respond(response)
        }
    }
}
