package io.rewynd.api.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.rewynd.api.UserSession
import io.rewynd.api.plugins.mkAdminAuthZPlugin
import io.rewynd.common.database.Database
import io.rewynd.common.decoder
import io.rewynd.common.generateSalt
import io.rewynd.common.hashPassword
import io.rewynd.common.model.ServerUser
import io.rewynd.model.ChangePasswordRequest
import io.rewynd.model.CreateUserRequest
import io.rewynd.model.DeleteUsersRequest
import io.rewynd.model.User
import io.rewynd.model.UserPreferences
import java.security.MessageDigest

fun Route.userRoutes(db: Database) {
    route("/user") {
        route("/changePassword") {
            post {
                val req = call.receive<ChangePasswordRequest>()
                call.sessions.get<UserSession>()?.username?.let { username ->
                    db.getUser(username)
                }?.let {
                    val oldHash = hashPassword(req.oldPassword, it.salt)
                    val newSalt = generateSalt()
                    val newHash = hashPassword(req.newPassword, newSalt)
                    if (MessageDigest.isEqual(
                            decoder.decode(oldHash),
                            decoder.decode(it.hashedPass),
                        ) && req.oldPassword != req.newPassword
                    ) {
                        db.upsertUser(it.copy(salt = newSalt, hashedPass = newHash))
                        call.respond(HttpStatusCode.OK)
                    } else {
                        null
                    }
                } ?: call.respond(HttpStatusCode.BadRequest)
            }
        }
        route("/list") {
            install(mkAdminAuthZPlugin(db))

            get {
                call.respond(db.listUsers().map { it.user })
            }
        }

        route("/get") {
            install(mkAdminAuthZPlugin(db))

            get("/{username}") {
                val user = call.parameters["username"]?.let { db.getUser(it) }?.user
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(user)
                }
            }
        }

        route("/create") {
            install(mkAdminAuthZPlugin(db))

            post {
                val req = call.receive<CreateUserRequest>()
                val salt = generateSalt()
                val hashedPass = hashPassword(req.password, salt)
                db.upsertUser(ServerUser(User(req.username, req.permissions, UserPreferences(false)), hashedPass, salt))
                call.respond(HttpStatusCode.OK)
            }
        }

        route("/delete") {
            install(mkAdminAuthZPlugin(db))

            post {
                val req = call.receive<DeleteUsersRequest>()
                req.users.forEach {
                    db.deleteUser(it)
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
