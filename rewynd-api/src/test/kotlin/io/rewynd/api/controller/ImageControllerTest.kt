package io.rewynd.api.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.next
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.util.toByteArray
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rewynd.api.ADMIN_USER
import io.rewynd.api.BaseHarness
import io.rewynd.api.SESSION_ID
import io.rewynd.api.plugins.configureSession
import io.rewynd.common.JSON
import io.rewynd.common.cache.Cache
import io.rewynd.common.cache.queue.ImageJobQueue
import io.rewynd.common.cache.queue.JobId
import io.rewynd.common.cache.queue.WorkerEvent
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerImageInfo
import io.rewynd.common.model.ServerUser
import io.rewynd.test.ApiGenerators
import io.rewynd.test.InternalGenerators
import io.rewynd.test.checkAllRun
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.encodeToString
import net.kensand.kielbasa.kotest.property.Generators

internal class ImageControllerTest : StringSpec({
    "getImage - cached" {
        Harness.arb.checkAllRun {
            testCall(
                {
                    getImage(imageInfo.imageId)
                },
                setup = { setupApp(db, cache, queue) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body().toByteArray() shouldBe byteArr
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
        Harness.arb.checkAllRun {
            coEvery { cache.getImage(imageInfo.imageId) } returns null
            testCall(
                {
                    getImage(imageInfo.imageId)
                },
                setup = { setupApp(db, cache, queue) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body().toByteArray() shouldBe byteArr
                coVerify {
                    cache.getImage(imageInfo.imageId)
                    queue.submit(imageInfo)
                }
            }
        }
    }

    "getImage - not found" {
        Harness.arb.checkAllRun {
            coEvery { cache.getImage(imageInfo.imageId) } returns null
            coEvery { db.getImage(imageInfo.imageId) } returns null
            testCall(
                {
                    getImage(imageInfo.imageId)
                },
                setup = { setupApp(db, cache, queue) },
            ) {
                status shouldBe HttpStatusCode.NotFound.value
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
        Harness.arb.checkAllRun {
            coEvery { cache.getImage(imageInfo.imageId) } returns null
            coEvery { db.getImage(imageInfo.imageId) } returns imageInfo
            coEvery { queue.monitor(jobId) } returns flowOf(WorkerEvent.Fail("FooReason"))

            testCall(
                {
                    getImage(imageInfo.imageId)
                },
                setup = { setupApp(db, cache, queue) },
            ) {
                status shouldBe HttpStatusCode.InternalServerError.value
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
        private class Harness(
            user: ServerUser = ADMIN_USER,
            sessionId: String = SESSION_ID,
            val byteArr: ByteArray = Generators.byteArray.next(),
            val imageInfo: ServerImageInfo = InternalGenerators.serverImageInfo.next(),
            val jobId: JobId = InternalGenerators.jobId.next(),
        ) : BaseHarness(user, sessionId) {
            val queue: ImageJobQueue = mockk {}

            init {
                coEvery { queue.submit(any()) } returns jobId
                coEvery { queue.monitor(jobId) } returns flowOf(WorkerEvent.Success(JSON.encodeToString(byteArr)))
                coEvery { cache.getImage(imageInfo.imageId) } returns byteArr
                coEvery { db.getImage(imageInfo.imageId) } returns imageInfo
                coEvery { cache.expireImage(any(), any()) } returns Unit
            }

            companion object {
                val arb =
                    arbitrary {
                        Harness(
                            user = InternalGenerators.serverUser.bind(),
                            sessionId = ApiGenerators.sessionId.bind(),
                            byteArr = Generators.byteArray.bind(),
                            imageInfo = InternalGenerators.serverImageInfo.bind(),
                            jobId = InternalGenerators.jobId.bind(),
                        )
                    }
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
