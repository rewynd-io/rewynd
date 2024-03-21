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
import io.rewynd.common.model.ServerShowInfo
import io.rewynd.model.ShowInfo
import io.rewynd.test.InternalGenerators
import io.rewynd.test.list
import io.rewynd.test.mockDatabase
import kotlinx.coroutines.runBlocking
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

internal class ShowControllerTest : StringSpec({
    "getShow" {
        val show = InternalGenerators.serverShowInfo.next()
        val db =
            mockDatabase(getShowHandler = {
                it shouldBe SHOW_ID
                show
            })
        testApplication {
            setupApp(db)
            inspecting(
                mkClient().get("/api/show/get/$SHOW_ID") {
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.OK
                runBlocking {
                    body<ShowInfo>() shouldBe show.toShowInfo()
                }
            }
        }
    }

    "listShows" {
        val shows = InternalGenerators.serverShowInfo.list().next()
        val db =
            mockDatabase(listShowsHandler = {
                it shouldBe SHOW_ID
                shows
            })
        testApplication {
            setupApp(db)
            inspecting(
                mkClient().get("/api/show/list/$SHOW_ID") {
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.OK
                runBlocking {
                    body<List<ShowInfo?>>() shouldBe shows.map(ServerShowInfo::toShowInfo)
                }
            }
        }
    }
}) {
    companion object {
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
                        showRoutes(db)
                    }
                }
            }
        }
    }
}
