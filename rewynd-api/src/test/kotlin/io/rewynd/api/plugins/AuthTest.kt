package io.rewynd.api.plugins

import io.kotest.core.extensions.install
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.bind
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.rewynd.api.BaseHarness
import io.rewynd.api.mkClient
import io.rewynd.api.setIsAdmin
import io.rewynd.common.model.ServerUser
import io.rewynd.test.InternalGenerators
import io.rewynd.test.MemorySessionStorage
import io.rewynd.test.UtilGenerators
import io.rewynd.test.checkAllRun

class AuthTest : StringSpec({
    "AuthNPlugin" {
        Harness.arb.checkAllRun {
            coEvery {
                db.mkSessionStorage()
            } returns emptySessionStorage
            testApplication {
                application {
                    configureSession(db)
                    routing {
                        install(mkAuthNPlugin())
                        route("/api/auth/login") {
                            get {
                                call.respond(HttpStatusCode.OK)
                            }
                        }
                        route("/someOtherRoute") {
                            get {
                                call.respond(HttpStatusCode.OK)
                            }
                        }
                    }
                }

                client.get("/api/auth/login").status shouldBe HttpStatusCode.OK
                client.get("/someOtherRoute").status shouldBe HttpStatusCode.Forbidden
            }
        }
    }

    "AdminAuthZPlugin - admin user" {
        Harness.adminArb.checkAllRun {
            testApplication {
                application {
                    configureSession(db)
                    routing {
                        route("/admin") {
                            install(mkAdminAuthZPlugin(db))
                            get {
                                call.respond(HttpStatusCode.OK)
                            }
                        }
                        route("/notAdmin") {
                            get {
                                call.respond(HttpStatusCode.OK)
                            }
                        }
                    }
                }
                val customClient = mkClient(cookieStorage)
                customClient.get("/admin").status shouldBe HttpStatusCode.OK
                customClient.get("/notAdmin").status shouldBe HttpStatusCode.OK
            }
        }
    }

    "AdminAuthZPlugin - non-admin user" {
        Harness.nonAdminArb.checkAllRun {
            testApplication {
                application {
                    configureSession(db)
                    routing {
                        route("/admin") {
                            install(mkAdminAuthZPlugin(db))
                            get {
                                call.respond(HttpStatusCode.OK)
                            }
                        }
                        route("/notAdmin") {
                            get {
                                call.respond(HttpStatusCode.OK)
                            }
                        }
                    }
                }
                val customClient = mkClient(cookieStorage)
                customClient.get("/admin").status shouldBe HttpStatusCode.Forbidden
                customClient.get("/notAdmin").status shouldBe HttpStatusCode.OK
            }
        }
    }
}) {
    companion object {
        data class Harness(
            val userParam: ServerUser,
            val sessionIdParam: String,
        ) : BaseHarness(userParam, sessionIdParam) {
            val emptySessionStorage = MemorySessionStorage()

            companion object {
                val adminArb =
                    arbitrary {
                        Harness(
                            InternalGenerators.serverUser.bind().setIsAdmin(true),
                            UtilGenerators.string.bind(),
                        )
                    }
                val nonAdminArb =
                    arbitrary {
                        Harness(
                            InternalGenerators.serverUser.bind().setIsAdmin(false),
                            UtilGenerators.string.bind(),
                        )
                    }
                val arb = Arb.bind<Harness>()
            }
        }
    }
}
