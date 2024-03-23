package io.rewynd.api.controller

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.generateSessionId
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.mockkStatic
import io.rewynd.api.ADMIN_USER
import io.rewynd.api.BaseHarness
import io.rewynd.api.SESSION_ID
import io.rewynd.api.SESSION_SERIALIZER
import io.rewynd.api.UserSession
import io.rewynd.api.plugins.configureSession
import io.rewynd.common.database.Database
import io.rewynd.common.hashPassword
import io.rewynd.common.model.ServerUser
import io.rewynd.model.LoginRequest
import io.rewynd.test.InternalGenerators
import io.rewynd.test.MemorySessionStorage
import io.rewynd.test.UtilGenerators

internal class AuthControllerTest : StringSpec({

    "verify failure" {
        Harness().run {
            coEvery { db.getUser(any()) } returns null
            testCall<Any?>(
                "/api/auth/verify",
                method = HttpMethod.Get,
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.Forbidden
            }
        }
    }

    "verify success" {
        Harness().run {
            testCall<Any?>(
                "/api/auth/verify",
                method = HttpMethod.Get,
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK
            }
        }
    }

    "verify invalid sessionId" {
        Harness(sessionId = "InvalidSession").run {
            coEvery { db.getUser(any()) } returns null
            testCall<Any?>(
                "/api/auth/verify",
                method = HttpMethod.Get,
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.Forbidden
            }
        }
    }

    "logout logged-in user" {
        Harness().run {
            testCall<Any?>(
                "/api/auth/logout",
                method = HttpMethod.Post,
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK
                shouldThrow<NoSuchElementException> {
                    session.read(SESSION_ID)
                }
            }
        }
    }

    "logout logged-out user" {
        Harness(sessionId = "InvalidSessionId").run {
            testCall<Any?>(
                "/api/auth/logout",
                method = HttpMethod.Post,
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK
                shouldThrow<NoSuchElementException> {
                    session.read(SESSION_ID)
                }
            }
        }
    }

    "login success" {
        Harness().run {
            val sessionStore = MemorySessionStorage()
            coEvery { db.mkSessionStorage() } returns sessionStore
            coEvery { db.getUser(userWithPass.user.username) } returns userWithPass
            mockkStatic(::generateSessionId)
            coEvery { generateSessionId() } returns sessionId
            testCall<Any?>(
                "/api/auth/login",
                LoginRequest(userWithPass.user.username, password),
                method = HttpMethod.Post,
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK
                sessionStore.read(sessionId) shouldBe
                    SESSION_SERIALIZER.serialize(
                        UserSession(
                            sessionId,
                            userWithPass.user.username,
                        ),
                    )
                val cookie = cookieStorage.get(Url("https://localhost")).first { it.name == "RewyndIoSession" }.value
                sessionStore.read(cookie).shouldNotBeNull()
            }
        }
    }

    "login invalidPass" {
        Harness().run {
            val sessionStore = MemorySessionStorage()
            coEvery { db.mkSessionStorage() } returns sessionStore
            coEvery { db.getUser(userWithPass.user.username) } returns userWithPass
            mockkStatic(::generateSessionId)
            coEvery { generateSessionId() } returns sessionId
            testCall<Any?>(
                "/api/auth/login",
                LoginRequest(userWithPass.user.username, "InvalidPassword"),
                method = HttpMethod.Post,
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.Forbidden

                shouldThrow<NoSuchElementException> {
                    sessionStore.read(sessionId)
                }

                cookieStorage.get(Url("https://localhost")).shouldBeEmpty()
            }
        }
    }

    "login missing element" {
        Harness().run {
            val sessionStore = MemorySessionStorage()
            coEvery { db.mkSessionStorage() } returns sessionStore
            coEvery { db.getUser(userWithPass.user.username) } returns userWithPass
            mockkStatic(::generateSessionId)
            coEvery { generateSessionId() } returns sessionId

            listOf(
                LoginRequest(null, null),
                LoginRequest(null, password),
                LoginRequest(userWithPass.user.username, null),
            ).forAll {
                testCall<Any?>(
                    "/api/auth/login",
                    it,
                    method = HttpMethod.Post,
                    setup = { setupApp(db) },
                ) {
                    status shouldBe HttpStatusCode.BadRequest

                    shouldThrow<NoSuchElementException> {
                        sessionStore.read(sessionId)
                    }

                    cookieStorage.get(Url("https://localhost")).shouldBeEmpty()
                }
            }
        }
    }

    "no user found" {
        Harness().run {
            val sessionStore = MemorySessionStorage()
            coEvery { db.mkSessionStorage() } returns sessionStore
            coEvery { db.getUser(userWithPass.user.username) } returns null
            mockkStatic(::generateSessionId)
            coEvery { generateSessionId() } returns sessionId
            testCall<Any?>(
                "/api/auth/login",
                LoginRequest(userWithPass.user.username, password),
                method = HttpMethod.Post,
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.Forbidden

                shouldThrow<NoSuchElementException> {
                    sessionStore.read(sessionId)
                }

                cookieStorage.get(Url("https://localhost")).shouldBeEmpty()
            }
        }
    }
}) {
    companion object {
        private class Harness(
            user: ServerUser = ADMIN_USER,
            sessionId: String = SESSION_ID,
        ) : BaseHarness(user, sessionId) {
            val password by lazy { Arb.string(minSize = 2).next() }
            val salt by lazy { UtilGenerators.urlEncodedBase64.next() }
            val hashedPass by lazy { hashPassword(password, salt) }
            val userWithPass by lazy { user.copy(hashedPass = hashedPass, salt = salt) }
        }

        val user = InternalGenerators.serverUser.next()

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
