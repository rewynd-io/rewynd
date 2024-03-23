package io.rewynd.api

import io.kotest.assertions.inspecting
import io.kotest.property.arbitrary.arbitrary
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.cookie
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerUser
import io.rewynd.common.model.SessionStorage
import io.rewynd.test.InternalGenerators
import io.rewynd.test.MemorySessionStorage
import io.rewynd.test.UtilGenerators
import kotlinx.coroutines.runBlocking

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
    val user: ServerUser = ADMIN_USER,
    val sessionId: String = SESSION_ID,
) {
    val session: SessionStorage =
        MemorySessionStorage().apply {
            runBlocking {
                write(
                    SESSION_ID,
                    SESSION_SERIALIZER.serialize(
                        UserSession(
                            SESSION_ID,
                            user.user.username,
                        ),
                    ),
                )
            }
        }
    val db: Database =
        mockk<Database> {
            coEvery { mkSessionStorage() } returns session
            coEvery { getUser(user.user.username) } returns user
        }
    val cache by lazy { mockk<Database>() }

    inline fun <reified Req : Any?> testCall(
        path: String,
        request: Req? = null,
        method: HttpMethod = HttpMethod.Post,
        crossinline clientSetup: ApplicationTestBuilder.() -> HttpClient = { mkClient() },
        crossinline setup: ApplicationTestBuilder.() -> Unit = {},
        noinline inspector: suspend HttpResponse.() -> Unit = {},
    ) {
        testApplication {
            setup()
            val customClient = clientSetup()
            val res =
                customClient.request {
                    this.method = method
                    cookie("RewyndIoSession", SESSION_ID)
                    request?.let {
                        setBody(it)
                        contentType(ContentType.Application.Json)
                    }
                    url {
                        protocol = URLProtocol.HTTPS
                        path(path)
                    }
                }
            inspecting(res) { runBlocking { inspector() } }
        }
    }

    companion object {
        val arbitrary =
            arbitrary {
                BaseHarness(InternalGenerators.serverUser.bind(), UtilGenerators.string.bind())
            }
    }
}
