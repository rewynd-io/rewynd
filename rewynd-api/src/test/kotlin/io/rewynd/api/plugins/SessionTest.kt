package io.rewynd.api.plugins

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.checkAll
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import io.rewynd.api.Generators
import io.rewynd.api.SESSION_SERIALIZER
import io.rewynd.api.UserSession
import io.rewynd.api.mkClient
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerUser
import io.rewynd.test.MemorySessionStorage
import io.rewynd.test.checkAllRun

class SessionTest : StringSpec({
    "configureSession secure" {
        checkAll(Generators.userSession) { userSession ->
            Harness.arb.checkAllRun {
                testApplication {
                    application {
                        configureSession(db, secure = true)
                        routing {
                            get {
                                context.sessions.set(userSession)
                                call.respond(HttpStatusCode.OK)
                            }
                        }
                    }
                    mkClient(cookieStorage).get { }.run {
                        status shouldBe HttpStatusCode.OK
                    }
                }
                val sessionCookie =
                    cookieStorage.get(Url("https://localhost")).first { it.name == "RewyndIoSession" }
                sessionCookie.secure shouldBe true
                storage.read(sessionCookie.value) shouldBe SESSION_SERIALIZER.serialize(userSession)
            }
        }
    }

    "configureSession insecure" {
        Harness.arb.checkAllRun {
            testApplication {
                application {
                    configureSession(db, secure = false)
                    routing {
                        get {
                            context.sessions.set(userSession)
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }
                mkClient(cookieStorage).get { }.run {
                    status shouldBe HttpStatusCode.OK
                }
            }
            val sessionCookie =
                cookieStorage.get(Url("http://localhost")).first { it.name == "RewyndIoSessionInsecure" }
            sessionCookie.secure shouldBe false
            storage.read(sessionCookie.value) shouldBe SESSION_SERIALIZER.serialize(userSession)
        }
    }
}) {
    companion object {
        data class Harness(
            val userParam: ServerUser,
            val userSession: UserSession,
        ) {
            val storage = MemorySessionStorage()
            val cookieStorage = AcceptAllCookiesStorage()
            val db =
                mockk<Database> {
                    coEvery { mkSessionStorage() } returns storage
                }

            companion object {
                val arb = Arb.bind<Harness>()
            }
        }
    }
}
