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
import io.rewynd.api.BaseHarness
import io.rewynd.api.plugins.configureSession
import io.rewynd.common.database.Database
import io.rewynd.common.database.Paged
import io.rewynd.common.model.Progressed
import io.rewynd.common.model.ServerEpisodeInfo
import io.rewynd.common.model.ServerSeasonInfo
import io.rewynd.common.model.ServerUser
import io.rewynd.common.model.toEpisodeInfo
import io.rewynd.model.GetNextEpisodeRequest
import io.rewynd.model.GetNextEpisodeResponse
import io.rewynd.model.ListEpisodesByLastUpdatedRequest
import io.rewynd.model.ListEpisodesByLastUpdatedResponse
import io.rewynd.model.ListEpisodesRequest
import io.rewynd.model.ListEpisodesResponse
import io.rewynd.model.SortOrder
import io.rewynd.test.ApiGenerators
import io.rewynd.test.InternalGenerators
import io.rewynd.test.checkAllRun
import io.rewynd.test.list
import io.rewynd.test.nullable
import net.kensand.kielbasa.kotest.property.Generators

internal class EpisodeControllerTest : StringSpec({
    "getEpisode" {
        Harness.arb.checkAllRun {
            coEvery { db.getProgressedEpisode(episode.data.id, username) } returns episode

            testCall(
                { getEpisode(episode.data.id) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe episode.toEpisodeInfo()
            }

            coVerify {
                db.getProgressedEpisode(any(), username)
            }
        }
    }

    "nextEpisode" {
        Harness.arb.checkAllRun {
            coEvery { db.getNextProgressedEpisode(episode.data.id, SortOrder.Ascending, username) } returns otherEpisode

            testCall(
                { getNextEpisode(GetNextEpisodeRequest(episode.data.id, SortOrder.Ascending)) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe GetNextEpisodeResponse(otherEpisode.toEpisodeInfo())
            }
        }
    }

    "previousEpisode" {
        Harness.arb.checkAllRun {
            coEvery {
                db.getNextProgressedEpisode(episode.data.id, SortOrder.Descending, username)
            } returns otherEpisode

            testCall(
                { getNextEpisode(GetNextEpisodeRequest(episode.data.id, SortOrder.Descending)) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe GetNextEpisodeResponse(otherEpisode.toEpisodeInfo())
            }
        }
    }

    "listEpisodes" {
        Harness.arb.checkAllRun {
            coEvery {
                db.listProgressedEpisodes(season.seasonInfo.id, cursor, username)
            } returns Paged(episodes, episodes.lastOrNull()?.data?.id)

            testCall(
                { listEpisodes(ListEpisodesRequest(season.seasonInfo.id, cursor = cursor)) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe
                    ListEpisodesResponse(
                        episodes.map(Progressed<ServerEpisodeInfo>::toEpisodeInfo),
                        episodes.lastOrNull()?.data?.id
                    )
            }
        }
    }

    "listByLastUpdated" {
        Harness.arb.checkAllRun {
            coEvery {
                db.listProgressedEpisodesByLastUpdated(
                    any(),
                    any(),
                    any(),
                    username
                )
            } returns Paged(episodes, null)

            testCall(
                {
                    listEpisodesByLastUpdated(
                        ListEpisodesByLastUpdatedRequest(),
                    )
                },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe
                    ListEpisodesByLastUpdatedResponse(
                        episodes.map(Progressed<ServerEpisodeInfo>::toEpisodeInfo),
                    )
            }
        }
    }
}) {
    companion object {
        class Harness(
            user: ServerUser,
            sessionId: String,
            val episodes: List<Progressed<ServerEpisodeInfo>>,
            val episode: Progressed<ServerEpisodeInfo>,
            val otherEpisode: Progressed<ServerEpisodeInfo>,
            val season: ServerSeasonInfo,
            val cursor: String?,
        ) : BaseHarness(user, sessionId) {
            init {
                coEvery { db.getProgressedEpisode(episode.data.id, username) } returns episode
            }

            companion object {
                val arb =
                    arbitrary {
                        Harness(
                            user = InternalGenerators.serverUser.bind(),
                            sessionId = ApiGenerators.sessionId.bind(),
                            episodes = InternalGenerators.progressedServerEpisodeInfo.list().bind(),
                            episode = InternalGenerators.progressedServerEpisodeInfo.bind(),
                            otherEpisode = InternalGenerators.progressedServerEpisodeInfo.bind(),
                            season = InternalGenerators.serverSeasonInfo.bind(),
                            cursor = Generators.string.nullable().bind(),
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
