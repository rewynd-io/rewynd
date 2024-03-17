package io.rewynd.api.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.rewynd.api.UserSession
import io.rewynd.common.database.Database
import io.rewynd.common.model.SessionStorage
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.days

fun Application.configureSession(
    database: Database,
    secure: Boolean = true,
) = runBlocking {
    val dbSessionStorage = database.mkSessionStorage()
    install(Sessions) {
        cookie<UserSession>(
            if (secure) "RewyndIoSession" else "RewyndIoSessionInsecure",
            dbSessionStorage.toKtorSessionStorage(),
        ) {
            cookie.maxAgeInSeconds = 30.days.inWholeSeconds
            cookie.secure = secure
        }
    }
}

fun SessionStorage.toKtorSessionStorage(): io.ktor.server.sessions.SessionStorage =
    object : io.ktor.server.sessions.SessionStorage {
        override suspend fun invalidate(id: String) = this@toKtorSessionStorage.invalidate(id)

        override suspend fun read(id: String): String = this@toKtorSessionStorage.read(id)

        override suspend fun write(
            id: String,
            value: String,
        ) = this@toKtorSessionStorage.write(id, value)
    }
