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
import io.rewynd.common.cache.Cache
import io.rewynd.common.cache.queue.ImageJobQueue
import io.rewynd.common.cache.queue.WorkerEvent
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerUser
import io.rewynd.test.InternalGenerators
import io.rewynd.test.UtilGenerators
import io.rewynd.test.mockJobQueue
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

internal class ImageControllerTest : StringSpec({
    "getImage - cached" {
        Harness().run {
            testCall<Any?>(
                "/api/image/${imageInfo.imageId}",
                method = HttpMethod.Get,
                setup = { setupApp(db, cache, queue) },
            ) {
                status shouldBe HttpStatusCode.OK
                body<ByteArray>() shouldBe byteArr
                coVerify {
                    cache.getImage(imageInfo.imageId)
                }
                coVerify(inverse = true) {
                    queue.submit(imageInfo)
                }
            }
        }
    }
    "getImage - not cached" {
        Harness().run {
            coEvery { cache.getImage(imageInfo.imageId) } returns null
            testCall<Any?>(
                "/api/image/${imageInfo.imageId}",
                method = HttpMethod.Get,
                setup = { setupApp(db, cache, queue) },
            ) {
                status shouldBe HttpStatusCode.OK
                body<ByteArray>() shouldBe byteArr
                coVerify {
                    cache.getImage(imageInfo.imageId)
                    queue.submit(imageInfo)
                }
            }
        }
    }

    "getImage - not found" {
        Harness().run {
            coEvery { cache.getImage(imageInfo.imageId) } returns null
            coEvery { db.getImage(imageInfo.imageId) } returns null
            testCall<Any?>(
                "/api/image/${imageInfo.imageId}",
                method = HttpMethod.Get,
                setup = { setupApp(db, cache, queue) },
            ) {
                status shouldBe HttpStatusCode.NotFound
                coVerify {
                    cache.getImage(imageInfo.imageId)
                    db.getImage(imageInfo.imageId)
                }
                coVerify(inverse = true) {
                    queue.submit(imageInfo)
                }
            }
        }
    }

    "getImage - Job Error" {
        Harness().run {
            coEvery { cache.getImage(imageInfo.imageId) } returns null
            coEvery { db.getImage(imageInfo.imageId) } returns imageInfo
            coEvery { queue.monitor(jobId) } returns flowOf(WorkerEvent.Fail("FooReason"))

            testCall<Any?>(
                "/api/image/${imageInfo.imageId}",
                method = HttpMethod.Get,
                setup = { setupApp(db, cache, queue) },
            ) {
                status shouldBe HttpStatusCode.InternalServerError
                coVerify {
                    cache.getImage(imageInfo.imageId)
                    db.getImage(imageInfo.imageId)
                    queue.submit(imageInfo)
                }
            }
        }
    }
}) {
    companion object {
        private const val IMAGE_ID = "FooImageId"

        private class Harness(
            user: ServerUser = ADMIN_USER,
            sessionId: String = SESSION_ID,
        ) : BaseHarness(user, sessionId) {
            val byteArr = UtilGenerators.byteArray.next()
            val imageInfo by lazy { InternalGenerators.serverImageInfo.next() }
            val jobId by lazy { InternalGenerators.jobId.next() }
            val queue: ImageJobQueue =
                mockJobQueue(
                    submitHandler = { jobId },
                    monitorHandler = {
                        it shouldBe jobId
                        flowOf(WorkerEvent.Success(Json.encodeToString(byteArr)))
                    },
                )

            init {
                coEvery { cache.getImage(imageInfo.imageId) } returns byteArr
                coEvery { db.getImage(imageInfo.imageId) } returns imageInfo
                coEvery { cache.expireImage(any(), any()) } returns Unit
            }
        }

        private fun ApplicationTestBuilder.mkClient() =
            createClient {
                install(ClientContentNegotiation) {
                    json()
                }
            }

        private fun ApplicationTestBuilder.setupApp(
            db: Database,
            cache: Cache,
            queue: ImageJobQueue,
        ) {
            install(ContentNegotiation) {
                json()
            }
            application {
                configureSession(db)
                routing {
                    route("/api") {
                        imageRoutes(db, cache, queue)
                    }
                }
            }
        }
    }
}
