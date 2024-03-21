package io.rewynd.api.controller

import io.kotest.assertions.inspecting
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.next
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.rewynd.api.plugins.configureSession
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerSeasonInfo
import io.rewynd.model.SeasonInfo
import io.rewynd.test.InternalGenerators
import io.rewynd.test.list
import io.rewynd.test.mockDatabase
import kotlinx.coroutines.runBlocking
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

internal class SeasonControllerTest : StringSpec({
    "getSeason" {
        val season = InternalGenerators.serverSeasonInfo.next()
        val db =
            mockDatabase(getSeasonHandler = {
                it shouldBe SEASON_ID
                season
            })
        testApplication {
            setupApp(db)
            inspecting(
                mkClient().get("/api/season/get/$SEASON_ID") {
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.OK
                runBlocking {
                    body<SeasonInfo>() shouldBe season.seasonInfo
                }
            }
        }
    }

    "listSeasons" {
        val seasons = InternalGenerators.serverSeasonInfo.list().next()
        val db =
            mockDatabase(listSeasonsHandler = {
                it shouldBe SHOW_ID
                seasons
            })
        testApplication {
            setupApp(db)
            inspecting(
                mkClient().get("/api/season/list/$SHOW_ID") {
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.OK
                runBlocking {
                    body<List<SeasonInfo?>>() shouldBe seasons.map(ServerSeasonInfo::seasonInfo)
                }
            }
        }
    }
}) {
    companion object {
        private const val SEASON_ID = "FooSeasonId"
        private const val SHOW_ID = "FooShowId"

        private fun ApplicationTestBuilder.mkClient() =
            createClient {
                install(ClientContentNegotiation) {
                    json()
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
