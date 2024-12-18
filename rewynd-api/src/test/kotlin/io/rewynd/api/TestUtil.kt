package io.rewynd.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import io.rewynd.client.RewyndClient
import io.rewynd.common.cache.Cache
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerUser
import io.rewynd.common.model.SessionStorage
import io.rewynd.test.MemorySessionStorage
import kotlinx.coroutines.runBlocking
import org.openapitools.client.infrastructure.HttpResponse

fun ApplicationTestBuilder.mkClient(cookieStorage: CookiesStorage = AcceptAllCookiesStorage()) =
    createClient {
        install(HttpCookies) {
            storage = cookieStorage
        }
        install(ContentNegotiation) {
            json()
        }
    }

open class BaseHarness(
    val user: ServerUser,
    val sessionId: String,
) {
    val username: String
        get() = user.user.username

    val session =
        UserSession(
            sessionId,
            user.user.username,
        )

    val sessionStorage: SessionStorage =
        MemorySessionStorage().apply {
            runBlocking {
                write(
                    sessionId,
                    SESSION_SERIALIZER.serialize(session),
                )
            }
        }
    val db: Database =
        mockk<Database> {
            coEvery { mkSessionStorage() } returns sessionStorage
            coEvery { getUser(user.user.username) } returns user
        }

    val cache = mockk<Cache>()

    val cookieStorage by lazy {
        runBlocking {
            AcceptAllCookiesStorage().apply {
                addCookie(
                    Url("https://localhost"),
                    Cookie("RewyndIoSession", sessionId),
                )
            }
        }
    }

    inline fun <Res : Any> testCall(
        crossinline clientCall: suspend RewyndClient.() -> HttpResponse<Res>,
        crossinline setup: ApplicationTestBuilder.() -> Unit = {},
        crossinline clientSetup: ApplicationTestBuilder.() -> HttpClient = { mkClient(cookieStorage) },
        noinline inspector: suspend HttpResponse<Res>.() -> Unit = {},
    ) {
        testApplication {
            setup()
            val customClient = RewyndClient("https://localhost", clientSetup())
            val res = clientCall.invoke(customClient)
            with(res) { runBlocking { inspector() } }
        }
    }

    companion object
}

fun ServerUser.setIsAdmin(isAdmin: Boolean = true) = copy(
    user = user.copy(permissions = user.permissions.copy(isAdmin))
)
