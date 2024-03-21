package io.rewynd.api.controller

import io.kotest.assertions.inspecting
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.next
import io.kotest.property.checkAll
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockkStatic
import io.rewynd.api.plugins.configureSession
import io.rewynd.api.util.getNextEpisodeInSeason
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerEpisodeInfo
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.ListEpisodesByLastUpdatedOrder
import io.rewynd.model.ListEpisodesByLastUpdatedRequest
import io.rewynd.model.ListEpisodesByLastUpdatedResponse
import io.rewynd.test.InternalGenerators
import io.rewynd.test.list
import io.rewynd.test.mockDatabase
import kotlinx.coroutines.runBlocking
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

internal class EpisodeControllerTest : StringSpec({
    "getEpisode" {
        val episode = InternalGenerators.serverEpisodeInfo.next()
        val db =
            mockDatabase(getEpisodeHandler = {
                it shouldBe EPISODE_ID
                episode
            })
        testApplication {
            setupApp(db)
            inspecting(
                mkClient().get("/api/episode/get/$EPISODE_ID") {
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.OK
                runBlocking {
                    body<EpisodeInfo>() shouldBe episode.toEpisodeInfo()
                }
            }
        }
    }

    "nextEpisode" {
        val nextEp = InternalGenerators.serverEpisodeInfo.next()
        mockkStatic(::getNextEpisodeInSeason)
        coEvery { getNextEpisodeInSeason(any(), any(), false) } returns nextEp

        val db = mockDatabase()
        testApplication {
            setupApp(db)
            inspecting(
                mkClient().get("/api/episode/next/$EPISODE_ID") {
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.OK
                runBlocking {
                    body<EpisodeInfo>() shouldBe nextEp.toEpisodeInfo()
                }
            }
        }
    }

    "previousEpisode" {
        val prevEp = InternalGenerators.serverEpisodeInfo.next()
        mockkStatic(::getNextEpisodeInSeason)
        coEvery { getNextEpisodeInSeason(any(), any(), true) } returns prevEp

        val db = mockDatabase()
        testApplication {
            setupApp(db)
            inspecting(
                mkClient().get("/api/episode/previous/$EPISODE_ID") {
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.OK
                runBlocking {
                    body<EpisodeInfo>() shouldBe prevEp.toEpisodeInfo()
                }
            }
        }
    }

    "listEpisodes" {
        val episodes = InternalGenerators.serverEpisodeInfo.list().next()
        val db =
            mockDatabase(listEpisodesHandler = {
                it shouldBe SEASON_ID
                episodes
            })
        testApplication {
            setupApp(db)
            inspecting(
                mkClient().get("/api/episode/list/$SEASON_ID") {
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.OK
                runBlocking {
                    body<List<EpisodeInfo?>>() shouldBe episodes.map(ServerEpisodeInfo::toEpisodeInfo)
                }
            }
        }
    }

    "listByLastUpdated" {
        checkAll(InternalGenerators.serverEpisodeInfo.list()) { episodes ->
            val db =
                mockDatabase(listEpisodesByLastUpdatedHandler = { cursor, order ->
                    cursor shouldBe CURSOR
                    order shouldBe ListEpisodesByLastUpdatedOrder.Newest
                    episodes
                })
            testApplication {
                setupApp(db)
                inspecting(
                    mkClient().post("/api/episode/listByLastUpdated") {
                        setBody(
                            ListEpisodesByLastUpdatedRequest(
                                ListEpisodesByLastUpdatedOrder.Newest,
                                CURSOR.toString(),
                            ),
                        )
                        url {
                            protocol = URLProtocol.HTTPS
                        }
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                    },
                ) {
                    status shouldBe HttpStatusCode.OK
                    runBlocking {
                        body<ListEpisodesByLastUpdatedResponse>().let {
                            it.episodes shouldBe episodes.map(ServerEpisodeInfo::toEpisodeInfo)
                            it.cursor shouldBe episodes.lastOrNull()?.lastUpdated?.toEpochMilliseconds()?.toString()
                        }
                    }
                }
            }
        }
    }

    "listByLastUpdated NumberFormatException" {
        checkAll(InternalGenerators.serverEpisodeInfo.list()) { episodes ->
            val db = mockDatabase()
            testApplication {
                setupApp(db)
                inspecting(
                    mkClient().post("/api/episode/listByLastUpdated") {
                        setBody(
                            ListEpisodesByLastUpdatedRequest(
                                ListEpisodesByLastUpdatedOrder.Newest,
                                "abc123",
                            ),
                        )
                        url {
                            protocol = URLProtocol.HTTPS
                        }
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                    },
                ) {
                    status shouldBe HttpStatusCode.BadRequest
                }
            }
        }
    }
}) {
    companion object {
        private const val EPISODE_ID = "FooEpisodeId"
        private const val SEASON_ID = "FooSeasonId"
        private const val CURSOR = 1234567890L

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
                        episodeRoutes(db)
                    }
                }
            }
        }
    }
}
