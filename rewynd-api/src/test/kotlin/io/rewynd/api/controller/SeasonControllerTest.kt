package io.rewynd.api.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.arbitrary
import io.ktor.client.call.body
import io.ktor.client.request.get
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
import io.rewynd.common.model.ServerSeasonInfo
import io.rewynd.common.model.ServerShowInfo
import io.rewynd.common.model.ServerUser
import io.rewynd.model.ListSeasonsRequest
import io.rewynd.model.ListSeasonsResponse
import io.rewynd.test.ApiGenerators
import io.rewynd.test.InternalGenerators
import io.rewynd.test.checkAllRun
import io.rewynd.test.list

internal class SeasonControllerTest : StringSpec({
    "getSeason" {
        Harness.arb.checkAllRun {
            coEvery {
                db.getSeason(season.seasonInfo.id)
            } returns season
            testCall(
                { getSeasons(season.seasonInfo.id) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe season.seasonInfo
            }
            coVerify {
                db.getSeason(season.seasonInfo.id)
            }
        }
    }

    "listSeasons" {
        Harness.arb.checkAllRun {
            coEvery {
                db.listSeasons(listSeasonsRequest.showId, listSeasonsRequest.cursor)
            } returns seasons
            testCall(
                { listSeasons(listSeasonsRequest) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe
                    ListSeasonsResponse(
                        seasons.map { it.seasonInfo },
                        seasons.lastOrNull()?.seasonInfo?.id,
                    )
            }
            coVerify {
                db.listSeasons(listSeasonsRequest.showId, listSeasonsRequest.cursor)
            }
        }
    }
}) {
    companion object {
        private class Harness(
            user: ServerUser = ADMIN_USER,
            sessionId: String = SESSION_ID,
            val season: ServerSeasonInfo,
            val seasons: List<ServerSeasonInfo>,
            val show: ServerShowInfo,
            val listSeasonsRequest: ListSeasonsRequest,
        ) : BaseHarness(user, sessionId) {
            companion object {
                val arb =
                    arbitrary {
                        Harness(
                            InternalGenerators.serverUser.bind(),
                            ApiGenerators.sessionId.bind(),
                            InternalGenerators.serverSeasonInfo.bind(),
                            InternalGenerators.serverSeasonInfo.list().bind(),
                            InternalGenerators.serverShowInfo.bind(),
                            ApiGenerators.listSeasonsRequest.bind(),
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
                        seasonRoutes(db)
                    }
                }
            }
        }
    }
}
