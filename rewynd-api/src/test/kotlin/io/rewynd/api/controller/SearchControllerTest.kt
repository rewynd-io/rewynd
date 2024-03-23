package io.rewynd.api.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.next
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
import io.mockk.mockk
import io.rewynd.api.ADMIN_USER
import io.rewynd.api.BaseHarness
import io.rewynd.api.SESSION_ID
import io.rewynd.api.plugins.configureSession
import io.rewynd.common.cache.queue.SearchJobQueue
import io.rewynd.common.cache.queue.WorkerEvent
import io.rewynd.common.database.Database
import io.rewynd.common.model.SearchProps
import io.rewynd.common.model.ServerUser
import io.rewynd.test.ApiGenerators
import io.rewynd.test.InternalGenerators
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class SearchControllerTest : StringSpec({
    "search" {
        Harness().run {

            testCall(
                { search(req) },
                setup = { setupApp(db, queue) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe res
            }
            coVerify {
                queue.submit(SearchProps(req.text))
            }
        }
    }
}) {
    companion object {
        private class Harness(
            user: ServerUser = ADMIN_USER,
            sessionId: String = SESSION_ID,
        ) : BaseHarness(user, sessionId) {
            val req by lazy { ApiGenerators.searchRequest.next() }
            val res by lazy { ApiGenerators.searchResponse.next() }
            val jobId by lazy { InternalGenerators.jobId.next() }
            val queue: SearchJobQueue = mockk {}

            init {
                coEvery { queue.submit(SearchProps(req.text)) } returns jobId
                coEvery { queue.monitor(jobId) } returns flowOf(WorkerEvent.Success(Json.encodeToString(res)))
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
