package io.rewynd.api.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.next
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Cache
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.util.toByteArray
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rewynd.api.ADMIN_USER
import io.rewynd.api.BaseHarness
import io.rewynd.api.SESSION_ID
import io.rewynd.api.plugins.configureSession
import io.rewynd.api.util.toIndexM3u8
import io.rewynd.api.util.toStreamM3u8
import io.rewynd.api.util.toSubsM3u8
import io.rewynd.common.cache.queue.StreamJobQueue
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerUser
import io.rewynd.common.model.StreamMetadataWrapper
import io.rewynd.test.ApiGenerators
import io.rewynd.test.InternalGenerators
import io.rewynd.test.UtilGenerators

internal class StreamControllerTest : StringSpec({
    "getHlsIndexM3u8" {
        Harness().run {
            coEvery { cache.getStreamMetadata(streamId) } returns metadataWrapper
            testCall(
                { getHlsIndexM3u8(streamId) },
                setup = { setupApp(db, cache, streamJobQueue) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe metadata.toIndexM3u8(streamId)
            }
            coVerify {
                cache.getStreamMetadata(streamId)
            }
        }
    }

    "getHlsStreamM3u8" {
        Harness().run {
            coEvery { cache.getStreamMetadata(streamId) } returns metadataWrapper
            testCall(
                { getHlsStreamM3u8(streamId) },
                setup = { setupApp(db, cache, streamJobQueue) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe metadata.toStreamM3u8()
            }
            coVerify {
                cache.getStreamMetadata(streamId)
            }
        }
    }

    "getHlsSubsM3u8" {
        Harness().run {
            coEvery { cache.getStreamMetadata(streamId) } returns metadataWrapper
            testCall(
                { getHlsSubsM3u8(streamId) },
                setup = { setupApp(db, cache, streamJobQueue) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe metadata.subtitles?.toSubsM3u8()
            }
            coVerify {
                cache.getStreamMetadata(streamId)
            }
        }
    }

    "getInitMp4" {
        Harness().run {
            coEvery { cache.getInitMp4(streamId) } returns byteArr
            testCall(
                { getHlsInitStream(streamId) },
                setup = { setupApp(db, cache, streamJobQueue) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body().toByteArray() shouldBe byteArr
            }
            coVerify {
                cache.getInitMp4(streamId)
            }
        }
    }

    "getSegmentM4s" {
        Harness().run {
            coEvery { cache.getSegmentM4s(streamId, segmentId) } returns byteArr
            testCall(
                { getHlsSegment(streamId, segmentId.toString()) },
                setup = { setupApp(db, cache, streamJobQueue) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body().toByteArray() shouldBe byteArr
            }
            coVerify {
                cache.getSegmentM4s(streamId, segmentId)
            }
        }
    }

    "getSegmentSubtitles" {
        Harness().run {
            coEvery { cache.getStreamMetadata(streamId) } returns metadataWrapper
            testCall(
                { getHlsSubs(streamId, subtitleSegId.toString()) },
                setup = { setupApp(db, cache, streamJobQueue) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe subtitleMetadata.segments[subtitleSegId].content
            }
            coVerify {
                cache.getStreamMetadata(streamId)
            }
        }
    }

    "deleteStream" {
        Harness().run {
            coEvery { streamJobQueue.cancel(streamMapping.jobId) } returns Unit
            coEvery { cache.getSessionStreamMapping(sessionId) } returns streamMapping
            coEvery { cache.getStreamMetadata(streamMapping.streamId) } returns metadataWrapper
            coEvery { cache.delSegmentM4s(streamMapping.streamId, any()) } returns Unit
            coEvery { cache.delInitMp4(streamMapping.streamId) } returns Unit
            coEvery { cache.delStreamMetadata(streamMapping.streamId) } returns Unit
            coEvery { cache.delSessionStreamMapping(sessionId) } returns Unit
            testCall(
                { deleteStream(streamId) },
                setup = { setupApp(db, cache, streamJobQueue) },
            ) {
                status shouldBe HttpStatusCode.OK.value
            }
            coVerify {
                cache.getStreamMetadata(streamMapping.streamId)
                streamJobQueue.cancel(streamMapping.jobId)
                (0 until subtitleMetadata.segments.size).forEach {
                    cache.delSegmentM4s(streamMapping.streamId, it)
                }
                cache.delInitMp4(streamMapping.streamId)
                cache.delStreamMetadata(streamMapping.streamId)
                cache.delSessionStreamMapping(sessionId)
            }
        }
    }
}) {
    companion object {
        private class Harness(
            user: ServerUser = ADMIN_USER,
            sessionId: String = SESSION_ID,
        ) : BaseHarness(user, sessionId) {
            val streamJobQueue = mockk<StreamJobQueue> {}
            val jobId = InternalGenerators.jobId.next()
            val streamId = ApiGenerators.streamId.next()
            val subtitleMetadata = InternalGenerators.subtitleMetadata.next()
            val metadata =
                InternalGenerators.streamMetadata.next()
                    .copy(
                        subtitles = subtitleMetadata,
                    )
            val metadataWrapper = StreamMetadataWrapper(metadata)
            val byteArr = UtilGenerators.byteArray.next()
            val segmentId = UtilGenerators.int.next()
            val subtitleSegId = Arb.int(0 until subtitleMetadata.segments.size).next()
            val streamMapping = InternalGenerators.streamMapping.next()
        }

        private fun ApplicationTestBuilder.setupApp(
            db: Database,
            cache: io.rewynd.common.cache.Cache,
            streamJobQueue: StreamJobQueue,
        ) {
            install(ContentNegotiation) {
                json()
            }
            application {
                configureSession(db)
                routing {
                    route("/api") {
                        streamRoutes(db, cache, streamJobQueue)
                    }
                }
            }
        }
    }
}
