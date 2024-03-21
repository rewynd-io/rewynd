package io.rewynd.api

import io.kotest.property.arbitrary.next
import io.ktor.server.sessions.defaultSessionSerializer
import io.rewynd.model.UserPermissions
import io.rewynd.test.InternalGenerators

const val SESSION_ID = "sessionId"
val SESSION_SERIALIZER = defaultSessionSerializer<UserSession>()
val ADMIN_USER =
    InternalGenerators.serverUser.next().run {
        copy(user = user.copy(permissions = UserPermissions(isAdmin = true)))
    }
val NON_ADMIN_USER =
    InternalGenerators.serverUser.next().run {
        copy(user = user.copy(permissions = UserPermissions(isAdmin = false)))
    }
