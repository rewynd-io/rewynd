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
import io.mockk.mockkStatic
import io.rewynd.api.ADMIN_USER
import io.rewynd.api.BaseHarness
import io.rewynd.api.SESSION_ID
import io.rewynd.api.plugins.configureSession
import io.rewynd.api.util.getNextEpisodeInSeason
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerEpisodeInfo
import io.rewynd.common.model.ServerUser
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.ListEpisodesByLastUpdatedOrder
import io.rewynd.model.ListEpisodesByLastUpdatedRequest
import io.rewynd.model.ListEpisodesByLastUpdatedResponse
import io.rewynd.test.InternalGenerators
import io.rewynd.test.UtilGenerators
import io.rewynd.test.list

internal class EpisodeControllerTest : StringSpec({
    "getEpisode" {
        Harness().run {
            coEvery { db.getEpisode(episode.id) } returns episode

            testCall<Any?>(
                "/api/episode/get/${episode.id}",
                method = HttpMethod.Get,
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK
                body<EpisodeInfo>() shouldBe episode.toEpisodeInfo()
            }

            coVerify {
                db.getEpisode(any())
            }
        }
    }

    "nextEpisode" {
        val nextEp = InternalGenerators.serverEpisodeInfo.next()

        Harness().run {
            mockkStatic(::getNextEpisodeInSeason)
            coEvery { getNextEpisodeInSeason(db, episode, false) } returns nextEp

            testCall<Any?>(
                "/api/episode/next/${episode.id}",
                method = HttpMethod.Get,
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK
                body<EpisodeInfo>() shouldBe nextEp.toEpisodeInfo()
            }
        }
    }

    "previousEpisode" {
        val prevEp = InternalGenerators.serverEpisodeInfo.next()
        Harness().run {
            mockkStatic(::getNextEpisodeInSeason)
            coEvery { getNextEpisodeInSeason(db, episode, true) } returns prevEp

            testCall<Any?>(
                "/api/episode/previous/${episode.id}",
                method = HttpMethod.Get,
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK
                body<EpisodeInfo>() shouldBe prevEp.toEpisodeInfo()
            }
        }
    }

    "listEpisodes" {
        Harness().run {
            coEvery { db.listEpisodes(season.seasonInfo.id) } returns episodes

            testCall<Any?>(
                "/api/episode/list/${season.seasonInfo.id}",
                method = HttpMethod.Get,
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK
                body<List<EpisodeInfo>>() shouldBe episodes.map(ServerEpisodeInfo::toEpisodeInfo)
            }
        }
    }

    "listByLastUpdated" {
        Harness().run {
            coEvery { db.listEpisodesByLastUpdated(cursor, ListEpisodesByLastUpdatedOrder.Oldest) } returns episodes

            testCall<Any?>(
                "/api/episode/listByLastUpdated",
                setup = { setupApp(db) },
                request =
                    ListEpisodesByLastUpdatedRequest(
                        ListEpisodesByLastUpdatedOrder.Oldest,
                        cursor.toString(),
                    ),
            ) {
                status shouldBe HttpStatusCode.OK
                body<ListEpisodesByLastUpdatedResponse>() shouldBe
                    ListEpisodesByLastUpdatedResponse(
                        episodes.map(ServerEpisodeInfo::toEpisodeInfo),
                        episodes.maxByOrNull { it.lastUpdated }?.lastUpdated?.toEpochMilliseconds()
                            ?.toString(),
                    )
            }
        }
    }

    "listByLastUpdated NumberFormatException" {
        Harness().run {
            testCall<Any?>(
                "/api/episode/listByLastUpdated",
                setup = { setupApp(db) },
                request =
                    ListEpisodesByLastUpdatedRequest(
                        ListEpisodesByLastUpdatedOrder.Oldest,
                        "abc",
                    ),
            ) {
                status shouldBe HttpStatusCode.BadRequest
                coVerify(inverse = true) { db.listEpisodesByLastUpdated(any(), any()) }
            }
        }
    }
}) {
    companion object {
        class Harness(
            user: ServerUser = ADMIN_USER,
            sessionId: String = SESSION_ID,
        ) : BaseHarness(user, sessionId) {
            val episodes by lazy { InternalGenerators.serverEpisodeInfo.list().next() }
            val episode by lazy { InternalGenerators.serverEpisodeInfo.next() }
            val season by lazy { InternalGenerators.serverSeasonInfo.next() }
            val cursor by lazy { UtilGenerators.long.next() }

            init {
                coEvery { db.getEpisode(episode.id) } returns episode
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
