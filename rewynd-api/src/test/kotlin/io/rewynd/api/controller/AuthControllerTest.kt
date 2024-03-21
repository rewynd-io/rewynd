package io.rewynd.api.controller

import io.kotest.assertions.inspecting
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.defaultSessionSerializer
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.rewynd.api.UserSession
import io.rewynd.api.plugins.configureSession
import io.rewynd.common.database.Database
import io.rewynd.common.hashPassword
import io.rewynd.model.LoginRequest
import io.rewynd.test.InternalGenerators
import io.rewynd.test.MemorySessionStorage
import io.rewynd.test.UtilGenerators
import io.rewynd.test.mockDatabase
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

internal class AuthControllerTest : StringSpec({

    "verify failure" {
        val db = mockDatabase()
        testApplication {
            setupApp(db)

            inspecting(
                client.get("/api/auth/verify") {
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.Forbidden
            }
        }
    }
    "verify success" {
        val session =
            MemorySessionStorage().apply {
                write(SESSION_ID, sessionSerializer.serialize(UserSession(SESSION_ID, user.user.username)))
            }
        val db =
            mockDatabase(
                mkSessionStorageHandler = { session },
                getUserHandler = { if (it == user.user.username) user else null },
            )
        testApplication {
            setupApp(db)

            inspecting(
                client.get("/api/auth/verify") {
                    cookie("RewyndIoSession", SESSION_ID)
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.OK
            }
        }
    }

    "verify invalid sessionId" {
        val session =
            MemorySessionStorage().apply {
                write(SESSION_ID, sessionSerializer.serialize(UserSession(SESSION_ID, user.user.username)))
            }
        val db =
            mockDatabase(
                mkSessionStorageHandler = { session },
                getUserHandler = { if (it == user.user.username) user else null },
            )
        testApplication {
            setupApp(db)

            inspecting(
                client.get("/api/auth/verify") {
                    cookie("RewyndIoSession", "someOtherSessionId")
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.Forbidden
            }
        }
    }

    "logout logged-in user" {
        val session =
            MemorySessionStorage().apply {
                write(SESSION_ID, sessionSerializer.serialize(UserSession(SESSION_ID, user.user.username)))
            }
        val db =
            mockDatabase(
                mkSessionStorageHandler = { session },
            )
        testApplication {
            setupApp(db)

            inspecting(
                client.post("/api/auth/logout") {
                    cookie("RewyndIoSession", SESSION_ID)
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.OK
            }
        }

        shouldThrow<NoSuchElementException> {
            session.read(SESSION_ID)
        }
    }

    "logout logged-out user" {
        val session =
            MemorySessionStorage()
        val db =
            mockDatabase(
                mkSessionStorageHandler = { session },
            )
        testApplication {
            setupApp(db)

            inspecting(
                client.post("/api/auth/logout") {
                    cookie("RewyndIoSession", SESSION_ID)
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.OK
            }
        }

        shouldThrow<NoSuchElementException> {
            session.read(SESSION_ID)
        }
    }

    "login success" {
        checkAll(Arb.string(minSize = 2), UtilGenerators.urlEncodedBase64) { password, salt ->
            val hashedPass = hashPassword(password, salt)
            val userWithPass = user.copy(hashedPass = hashedPass, salt = salt)

            val session =
                MemorySessionStorage().apply {
                    write(SESSION_ID, sessionSerializer.serialize(UserSession(SESSION_ID, userWithPass.user.username)))
                }
            val db =
                mockDatabase(
                    mkSessionStorageHandler = { session },
                    getUserHandler = { if (it == userWithPass.user.username) userWithPass else null },
                )
            val cookieStorage = AcceptAllCookiesStorage()

            testApplication {
                val client =
                    createClient {
                        install(HttpCookies) {
                            storage = cookieStorage
                        }
                        install(ClientContentNegotiation) {
                            json()
                        }
                    }
                setupApp(db)

                inspecting(
                    client.post("/api/auth/login") {
                        url {
                            protocol = URLProtocol.HTTPS
                        }
                        setBody(LoginRequest(userWithPass.user.username, password))
                        contentType(ContentType.Application.Json)
                    },
                ) {
                    status shouldBe HttpStatusCode.OK
                }

                session.read(SESSION_ID) shouldBe
                    sessionSerializer.serialize(
                        UserSession(
                            SESSION_ID,
                            userWithPass.user.username,
                        ),
                    )
                val cookie = cookieStorage.get(Url("https://localhost")).first { it.name == "RewyndIoSession" }.value
                session.read(cookie).shouldNotBeNull()
            }
        }
    }

    "login invalidPass" {
        checkAll(Arb.string(minSize = 2), UtilGenerators.urlEncodedBase64) { password, salt ->
            val hashedPass = hashPassword(password + "SomethingElse", salt)
            val userWithPass = user.copy(hashedPass = hashedPass, salt = salt)

            val session = MemorySessionStorage()
            val db =
                mockDatabase(
                    mkSessionStorageHandler = { session },
                    getUserHandler = { if (it == userWithPass.user.username) userWithPass else null },
                )
            val cookieStorage = AcceptAllCookiesStorage()

            testApplication {
                val client =
                    createClient {
                        install(HttpCookies) {
                            storage = cookieStorage
                        }
                        install(ClientContentNegotiation) {
                            json()
                        }
                    }
                setupApp(db)

                inspecting(
                    client.post("/api/auth/login") {
                        url {
                            protocol = URLProtocol.HTTPS
                        }
                        setBody(LoginRequest(userWithPass.user.username, password))
                        contentType(ContentType.Application.Json)
                    },
                ) {
                    status shouldBe HttpStatusCode.Forbidden
                }

                shouldThrow<NoSuchElementException> {
                    session.read(SESSION_ID)
                }
                cookieStorage.get(Url("https://localhost")).shouldBeEmpty()
            }
        }
    }

    "login missing password" {
        listOf(
            LoginRequest(null, null),
            LoginRequest(null, "foo"),
            LoginRequest("foo", null),
        ).forAll {
            val session = MemorySessionStorage()
            val db = mockDatabase()
            val cookieStorage = AcceptAllCookiesStorage()

            testApplication {
                val client =
                    createClient {
                        install(HttpCookies) {
                            storage = cookieStorage
                        }
                        install(ClientContentNegotiation) {
                            json()
                        }
                    }
                setupApp(db)

                inspecting(
                    client.post("/api/auth/login") {
                        url {
                            protocol = URLProtocol.HTTPS
                        }
                        setBody(it)
                        contentType(ContentType.Application.Json)
                    },
                ) {
                    status shouldBe HttpStatusCode.BadRequest
                }

                shouldThrow<NoSuchElementException> {
                    session.read(SESSION_ID)
                }
                cookieStorage.get(Url("https://localhost")).shouldBeEmpty()
            }
        }
    }

    "no user found" {
        val session = MemorySessionStorage()
        val db = mockDatabase(getUserHandler = { null })
        val cookieStorage = AcceptAllCookiesStorage()

        testApplication {
            val client =
                createClient {
                    install(HttpCookies) {
                        storage = cookieStorage
                    }
                    install(ClientContentNegotiation) {
                        json()
                    }
                }
            setupApp(db)

            inspecting(
                client.post("/api/auth/login") {
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                    setBody(LoginRequest("non-existent-user", "bar"))
                    contentType(ContentType.Application.Json)
                },
            ) {
                status shouldBe HttpStatusCode.Forbidden
            }

            shouldThrow<NoSuchElementException> {
                session.read(SESSION_ID)
            }
            cookieStorage.get(Url("https://localhost")).shouldBeEmpty()
        }
    }
}) {
    companion object {
        val sessionSerializer = defaultSessionSerializer<UserSession>()
        val user = InternalGenerators.serverUser.next()
        const val SESSION_ID = "sessionId"

        private fun ApplicationTestBuilder.setupApp(db: Database) {
            install(ContentNegotiation) {
                json()
            }
            application {
                configureSession(db)
                routing {
                    route("/api") {
                        authRoutes(db)
                    }
                }
            }
        }
    }
}
