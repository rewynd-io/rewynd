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
import io.rewynd.common.cache.Cache
import io.rewynd.common.cache.queue.ImageJobQueue
import io.rewynd.common.cache.queue.WorkerEvent
import io.rewynd.common.database.Database
import io.rewynd.test.InternalGenerators
import io.rewynd.test.UtilGenerators
import io.rewynd.test.mockCache
import io.rewynd.test.mockDatabase
import io.rewynd.test.mockJobQueue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

internal class ImageControllerTest : StringSpec({
    "getImage - cached" {
        val byteArr = UtilGenerators.byteArray.next()
        val db = mockDatabase()
        val cache =
            mockCache(getImageHandler = {
                it shouldBe IMAGE_ID
                byteArr
            })
        val queue: ImageJobQueue = mockJobQueue()
        testApplication {
            setupApp(db, cache, queue)
            inspecting(
                mkClient().get("/api/image/$IMAGE_ID") {
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.OK
                runBlocking {
                    body<ByteArray>() shouldBe byteArr
                }
            }
        }
    }
    "getImage - not cached" {
        val byteArr = UtilGenerators.byteArray.next()
        val imageInfo = InternalGenerators.serverImageInfo.next()
        val jobId = InternalGenerators.jobId.next()

        val db =
            mockDatabase(getImageHandler = {
                it shouldBe IMAGE_ID
                imageInfo
            })
        val cache =
            mockCache(getImageHandler = {
                it shouldBe IMAGE_ID
                null
            })
        val queue: ImageJobQueue =
            mockJobQueue(
                submitHandler = {
                    it shouldBe imageInfo
                    jobId
                },
                monitorHandler = {
                    it shouldBe jobId
                    flowOf(WorkerEvent.Success(Json.encodeToString(byteArr)))
                },
            )
        testApplication {
            setupApp(db, cache, queue)
            inspecting(
                mkClient().get("/api/image/$IMAGE_ID") {
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.OK
                runBlocking {
                    body<ByteArray>() shouldBe byteArr
                }
            }
        }
    }

    "getImage - not found" {
        val db =
            mockDatabase(getImageHandler = {
                it shouldBe IMAGE_ID
                null
            })
        val cache =
            mockCache(getImageHandler = {
                it shouldBe IMAGE_ID
                null
            })
        val queue: ImageJobQueue = mockJobQueue()
        testApplication {
            setupApp(db, cache, queue)
            inspecting(
                mkClient().get("/api/image/$IMAGE_ID") {
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.NotFound
            }
        }
    }

    "getImage - Job Error" {
        val imageInfo = InternalGenerators.serverImageInfo.next()
        val jobId = InternalGenerators.jobId.next()

        val db =
            mockDatabase(getImageHandler = {
                it shouldBe IMAGE_ID
                imageInfo
            })
        val cache =
            mockCache(getImageHandler = {
                it shouldBe IMAGE_ID
                null
            })
        val queue: ImageJobQueue =
            mockJobQueue(
                submitHandler = { jobId },
                monitorHandler = { flowOf(WorkerEvent.Fail("FooReason")) },
            )
        testApplication {
            setupApp(db, cache, queue)
            inspecting(
                mkClient().get("/api/image/$IMAGE_ID") {
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.InternalServerError
            }
        }
    }
}) {
    companion object {
        private const val IMAGE_ID = "FooImageId"

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
