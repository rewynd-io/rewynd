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
import io.mockk.mockk
import io.rewynd.api.BaseHarness
import io.rewynd.api.plugins.configureSession
import io.rewynd.common.JSON
import io.rewynd.common.cache.queue.JobId
import io.rewynd.common.cache.queue.SearchJobQueue
import io.rewynd.common.cache.queue.WorkerEvent
import io.rewynd.common.database.Database
import io.rewynd.common.model.SearchProps
import io.rewynd.common.model.ServerUser
import io.rewynd.model.SearchRequest
import io.rewynd.model.SearchResponse
import io.rewynd.test.ApiGenerators
import io.rewynd.test.InternalGenerators
import io.rewynd.test.checkAllRun
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.encodeToString

internal class SearchControllerTest : StringSpec({
    "search" {
        Harness.arb.checkAllRun {
            testCall(
                { search(req) },
                setup = { setupApp(db, queue) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe
                    if (req.text.isBlank()) {
                        SearchResponse(emptyList())
                    } else {
                        res
                    }
            }
            coVerify(exactly = if (req.text.isBlank()) 0 else 1) {
                queue.submit(SearchProps(req.text))
            }
        }
    }
}) {
    companion object {
        private class Harness(
            user: ServerUser,
            sessionId: String,
            val req: SearchRequest,
            val res: SearchResponse,
            val jobId: JobId,
        ) : BaseHarness(user, sessionId) {
            val queue: SearchJobQueue = mockk {}

            init {
                coEvery { queue.submit(SearchProps(req.text)) } returns jobId
                coEvery { queue.monitor(jobId) } returns flowOf(WorkerEvent.Success(JSON.encodeToString(res)))
            }

            companion object {
                val arb =
                    arbitrary {
                        Harness(
                            InternalGenerators.serverUser.bind(),
                            ApiGenerators.sessionId.bind(),
                            ApiGenerators.searchRequest.bind(),
                            ApiGenerators.searchResponse.bind(),
                            InternalGenerators.jobId.bind(),
                        )
                    }
            }
        }

        private fun ApplicationTestBuilder.setupApp(
            db: Database,
            queue: SearchJobQueue,
        ) {
            install(ContentNegotiation) {
                json()
            }
            application {
                configureSession(db)
                routing {
                    route("/api") {
                        searchRoutes(queue)
                    }
                }
            }
        }
    }
}
