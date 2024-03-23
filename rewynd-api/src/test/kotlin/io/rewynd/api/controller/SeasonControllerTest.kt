package io.rewynd.api.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.next
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpMethod
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
import io.rewynd.common.model.ServerUser
import io.rewynd.model.SeasonInfo
import io.rewynd.test.InternalGenerators
import io.rewynd.test.list

internal class SeasonControllerTest : StringSpec({
    "getSeason" {
        Harness().run {
            coEvery {
                db.getSeason(season.seasonInfo.id)
            } returns season
            testCall<Any?>(
                "/api/season/get/${season.seasonInfo.id}",
                method = HttpMethod.Get,
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK
                body<SeasonInfo>() shouldBe season.seasonInfo
            }
            coVerify {
                db.getSeason(season.seasonInfo.id)
            }
        }
    }

    "listSeasons" {
        Harness().run {
            coEvery {
                db.listSeasons(show.id)
            } returns seasons
            testCall<Any?>(
                "/api/season/list/${show.id}",
                method = HttpMethod.Get,
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK
                body<List<SeasonInfo>>() shouldBe seasons.map(ServerSeasonInfo::seasonInfo)
            }
            coVerify {
                db.listSeasons(show.id)
            }
        }
    }
}) {
    companion object {
        private class Harness(
            user: ServerUser = ADMIN_USER,
            sessionId: String = SESSION_ID,
        ) : BaseHarness(user, sessionId) {
            val season by lazy { InternalGenerators.serverSeasonInfo.next() }
            val seasons by lazy { InternalGenerators.serverSeasonInfo.list().next() }
            val show by lazy { InternalGenerators.serverShowInfo.next() }
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
