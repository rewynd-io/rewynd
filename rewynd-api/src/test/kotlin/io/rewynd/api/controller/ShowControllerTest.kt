package io.rewynd.api.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.next
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.rewynd.api.ADMIN_USER
import io.rewynd.api.BaseHarness
import io.rewynd.api.SESSION_ID
import io.rewynd.api.plugins.configureSession
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerShowInfo
import io.rewynd.common.model.ServerUser
import io.rewynd.model.Library
import io.rewynd.test.ApiGenerators
import io.rewynd.test.InternalGenerators
import io.rewynd.test.checkAllRun
import io.rewynd.test.list

internal class ShowControllerTest : StringSpec({
    "getShow" {
        Harness.arb.checkAllRun {
            coEvery {
                db.getShow(show.id)
            } returns show
            testCall(
                { getShow(show.id) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe show.toShowInfo()
            }
            coVerify {
                db.getShow(show.id)
            }
        }
    }

    "listShows" {
        Harness.arb.checkAllRun {
            coEvery {
                db.listShows(library.name)
            } returns shows
            testCall(
                { listShows(library.name) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe shows.map(ServerShowInfo::toShowInfo)
            }
            coVerify {
                db.listShows(library.name)
            }
        }
    }
}) {
    companion object {
        private class Harness(
            user: ServerUser = ADMIN_USER,
            sessionId: String = SESSION_ID,
            val show: ServerShowInfo = InternalGenerators.serverShowInfo.next(),
            val shows: List<ServerShowInfo> = InternalGenerators.serverShowInfo.list().next(),
            val library: Library = ApiGenerators.library.next(),
        ) : BaseHarness(user, sessionId) {
            companion object {
                val arb =
                    arbitrary {
                        Harness(
                            InternalGenerators.serverUser.bind(),
                            ApiGenerators.sessionId.bind(),
                            InternalGenerators.serverShowInfo.bind(),
                            InternalGenerators.serverShowInfo.list().bind(),
                            ApiGenerators.library.bind(),
                        )
                    }
            }
        }

        private fun ApplicationTestBuilder.setupApp(db: Database) {
            install(ContentNegotiation) {
                json()
            }
            application {
                configureSession(db)
                routing {
                    route("/api") {
                        showRoutes(db)
                    }
                }
            }
        }
    }
}
