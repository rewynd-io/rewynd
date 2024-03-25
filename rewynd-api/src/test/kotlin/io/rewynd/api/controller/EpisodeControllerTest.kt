package io.rewynd.api.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.arbitrary
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkStatic
import io.rewynd.api.BaseHarness
import io.rewynd.api.plugins.configureSession
import io.rewynd.api.util.getNextEpisodeInSeason
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerEpisodeInfo
import io.rewynd.common.model.ServerSeasonInfo
import io.rewynd.common.model.ServerUser
import io.rewynd.model.ListEpisodesByLastUpdatedOrder
import io.rewynd.model.ListEpisodesByLastUpdatedRequest
import io.rewynd.model.ListEpisodesByLastUpdatedResponse
import io.rewynd.model.ListEpisodesRequest
import io.rewynd.model.ListEpisodesResponse
import io.rewynd.test.ApiGenerators
import io.rewynd.test.InternalGenerators
import io.rewynd.test.UtilGenerators
import io.rewynd.test.checkAllRun
import io.rewynd.test.list

internal class EpisodeControllerTest : StringSpec({
    "getEpisode" {
        Harness.arb.checkAllRun {
            coEvery { db.getEpisode(episode.id) } returns episode

            testCall(
                { getEpisode(episode.id) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe episode.toEpisodeInfo()
            }

            coVerify {
                db.getEpisode(any())
            }
        }
    }

    "nextEpisode" {
        Harness.arb.checkAllRun {
            mockkStatic(::getNextEpisodeInSeason)
            coEvery { getNextEpisodeInSeason(db, episode, false) } returns otherEpisode

            testCall(
                { getNextEpisode(episode.id) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe otherEpisode.toEpisodeInfo()
            }
        }
    }

    "previousEpisode" {
        Harness.arb.checkAllRun {
            mockkStatic(::getNextEpisodeInSeason)
            coEvery { getNextEpisodeInSeason(db, episode, true) } returns otherEpisode

            testCall(
                { getPreviousEpisode(episode.id) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe otherEpisode.toEpisodeInfo()
            }
        }
    }

    "listEpisodes" {
        Harness.arb.checkAllRun {
            coEvery { db.listEpisodes(season.seasonInfo.id) } returns episodes

            testCall(
                { listEpisodes(ListEpisodesRequest(season.seasonInfo.id)) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe ListEpisodesResponse(episodes.map(ServerEpisodeInfo::toEpisodeInfo), episodes.lastOrNull()?.id)
            }
        }
    }

    "listByLastUpdated" {
        Harness.arb.checkAllRun {
            coEvery { db.listEpisodesByLastUpdated(cursor, ListEpisodesByLastUpdatedOrder.Oldest) } returns episodes

            testCall(
                {
                    listEpisodesByLastUpdated(
                        ListEpisodesByLastUpdatedRequest(
                            ListEpisodesByLastUpdatedOrder.Oldest,
                            cursor.toString(),
                        ),
                    )
                },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe
                    ListEpisodesByLastUpdatedResponse(
                        episodes.map(ServerEpisodeInfo::toEpisodeInfo),
                        episodes.maxByOrNull { it.lastUpdated }?.lastUpdated?.toEpochMilliseconds()
                            ?.toString(),
                    )
            }
        }
    }

    "listByLastUpdated NumberFormatException" {
        Harness.arb.checkAllRun {
            testCall(
                {
                    listEpisodesByLastUpdated(
                        ListEpisodesByLastUpdatedRequest(
                            ListEpisodesByLastUpdatedOrder.Oldest,
                            "abc",
                        ),
                    )
                },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.BadRequest.value
                coVerify(inverse = true) { db.listEpisodesByLastUpdated(any(), any()) }
            }
        }
    }
}) {
    companion object {
        class Harness(
            user: ServerUser,
            sessionId: String,
            val episodes: List<ServerEpisodeInfo>,
            val episode: ServerEpisodeInfo,
            val otherEpisode: ServerEpisodeInfo,
            val season: ServerSeasonInfo,
            val cursor: Long,
        ) : BaseHarness(user, sessionId) {
            init {
                coEvery { db.getEpisode(episode.id) } returns episode
            }

            companion object {
                val arb =
                    arbitrary {
                        Harness(
                            user = InternalGenerators.serverUser.bind(),
                            sessionId = ApiGenerators.sessionId.bind(),
                            episodes = InternalGenerators.serverEpisodeInfo.list().bind(),
                            episode = InternalGenerators.serverEpisodeInfo.bind(),
                            otherEpisode = InternalGenerators.serverEpisodeInfo.bind(),
                            season = InternalGenerators.serverSeasonInfo.bind(),
                            cursor = UtilGenerators.long.bind(),
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
                        episodeRoutes(db)
                    }
                }
            }
        }
    }
}
