package io.rewynd.api.controller

import arrow.fx.coroutines.parMapUnordered
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.defaultForFileExtension
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.sessionId
import io.rewynd.api.UserSession
import io.rewynd.api.util.toIndexM3u8
import io.rewynd.api.util.toStreamM3u8
import io.rewynd.api.util.toSubsM3u8
import io.rewynd.common.cache.Cache
import io.rewynd.common.cache.queue.StreamJobQueue
import io.rewynd.common.cache.withLock
import io.rewynd.common.database.Database
import io.rewynd.common.model.ClientStreamEvents
import io.rewynd.common.model.ServerMediaInfo
import io.rewynd.common.model.StreamMapping
import io.rewynd.common.model.StreamMetadata
import io.rewynd.common.model.StreamMetadataWrapper
import io.rewynd.common.model.StreamProps
import io.rewynd.common.partialSeconds
import io.rewynd.model.CreateStreamRequest
import io.rewynd.model.HlsStreamProps
import io.rewynd.model.LibraryType
import io.rewynd.model.StreamHeartbeatResponse
import io.rewynd.model.StreamStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import java.lang.Double.max
import java.util.UUID
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val log by lazy { KotlinLogging.logger { } }

@Suppress("LongMethod")
fun Route.streamRoutes(
    db: Database,
    cache: Cache,
    queue: StreamJobQueue,
) {
    get("/stream/{streamId}/{segmentId}.m4s") {
        call.parameters["streamId"]?.let { streamId ->
            call.parameters["segmentId"]?.toIntOrNull()?.let { segmentId ->
                cache.getSegmentM4s(streamId, segmentId)?.let {
                    call.respondBytes(it, ContentType("video", "mp4"))
                }
            }
        } ?: call.respond(HttpStatusCode.BadRequest)
    }
    get("/stream/{streamId}/{segmentId}.vtt") {
        call.parameters["streamId"]?.let { streamId ->
            call.parameters["segmentId"]?.toIntOrNull()?.let { segmentId ->
                cache.getStreamMetadata(streamId)?.streamMetadata?.subtitles?.segments?.get(
                    segmentId,
                )?.let { call.respondText(it.content) }
            }
        } ?: call.respond(HttpStatusCode.BadRequest)
    }
    get("/stream/{streamId}/stream.m3u8") {
        call.parameters["streamId"]?.let { streamId ->
            cache.getStreamMetadata(streamId)?.streamMetadata?.let {
                call.response.header("Content-Type", "application/vnd.apple.mpegurl")
                call.respondText(
                    it.toStreamM3u8(),
                    ContentType.defaultForFileExtension("m3u8"),
                )
            }
        } ?: call.respond(HttpStatusCode.BadRequest)
    }
    get("/stream/{streamId}/index.m3u8") {
        call.parameters["streamId"]?.let { streamId ->
            cache.getStreamMetadata(streamId)?.streamMetadata?.let {
                call.respondText(ContentType.defaultForFileExtension("m3u8")) { it.toIndexM3u8(streamId) }
            } ?: call.respond(HttpStatusCode.BadRequest)
        }
    }
    get("/stream/{streamId}/subs.m3u8") {
        call.parameters["streamId"]?.let { streamId ->
            cache.getStreamMetadata(streamId)?.streamMetadata?.subtitles?.let { subtitleMetadata ->
                call.respondText(ContentType.defaultForFileExtension("m3u8")) { subtitleMetadata.toSubsM3u8() }
            }
        } ?: call.respond(HttpStatusCode.BadRequest)
    }
    get("/stream/{streamId}/init-stream.mp4") {
        call.parameters["streamId"]?.let { streamId ->
            cache.getInitMp4(streamId)?.let {
                call.respondBytes(it, ContentType("video", "mp4"))
            }
        } ?: call.respond(HttpStatusCode.BadRequest)
    }
    // TODO no sense sending the streamId if it's not used
    delete("/stream/delete/{streamId}") {
        call.parameters["streamId"]?.let { streamId ->
            call.sessionId<UserSession>()?.let { sessionId ->
                cache.getSessionStreamMapping(sessionId)?.let { streamMapping ->
                    deleteStream(queue, streamMapping, cache, sessionId)
                    call.respond(HttpStatusCode.OK)
                } ?: call.respond(HttpStatusCode.NotFound)
            } ?: call.respond(HttpStatusCode.Forbidden)
        } ?: call.respond(HttpStatusCode.BadRequest)
    }
    post("/stream/create") {
        runBlocking {
            val req: CreateStreamRequest = call.receive()
            val id = UUID.randomUUID().toString()
            db.getLibrary(req.library)?.let { library ->
                call.sessionId<UserSession>()?.let { sessionId ->
                    when (library.type) {
                        LibraryType.Show -> db.getEpisode(req.id)?.toServerMediaInfo()
                        LibraryType.Movie -> db.getMovie(req.id)?.toServerMediaInfo()
                        LibraryType.Image -> TODO()
                    }?.let { serverMediaInfo ->
                        // Lock to prevent parallel creation of streams using the same session
                        createStream(cache, sessionId, queue, id, serverMediaInfo, req)
                        call.respond(
                            HlsStreamProps(
                                id = id,
                                url = "/api/stream/$id/index.m3u8",
                                duration = serverMediaInfo.mediaInfo.runTime,
                            ),
                        )
                    }
                }
            }
        } ?: call.respond(HttpStatusCode.InternalServerError)
    }
    post("/stream/heartbeat/{streamId}") {
        val reqStreamId = requireNotNull(call.parameters["streamId"]) { "Missing StreamId" }
        val sessionId = call.sessionId<UserSession>()
        val metadata = cache.getStreamMetadata(reqStreamId)
        val streamMetadata = metadata?.streamMetadata
        if (metadata == null) {
            call.respond(StreamHeartbeatResponse(StreamStatus.Canceled, 0.0))
        } else if (streamMetadata == null) {
            call.respond(StreamHeartbeatResponse(StreamStatus.Pending, 0.0))
        } else {
            val expire = Clock.System.now() + 2.minutes
            val allItemsExist = cache.heartbeatStream(streamMetadata, reqStreamId, expire, sessionId, queue)
            if (!allItemsExist) {
                log.info { "Stream Status: ${StreamStatus.Canceled}" }
                call.respond(StreamHeartbeatResponse(StreamStatus.Canceled, 0.0))
            } else if (streamMetadata.segments.isNotEmpty() && (
                    streamMetadata.streamProps.subtitleStreamName == null || streamMetadata.subtitles != null
                    )
            ) {
                log.info { "Stream Status: ${StreamStatus.Available}" }
                call.respond(
                    StreamHeartbeatResponse(
                        StreamStatus.Available,
                        streamMetadata.actualStartOffset.partialSeconds
                    )
                )
            } else {
                log.info { "Stream Status: ${StreamStatus.Pending}" }
                call.respond(StreamHeartbeatResponse(StreamStatus.Pending, 0.0))
            }
        }
    }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
private suspend fun Cache.heartbeatStream(
    streamMetadata: StreamMetadata,
    reqStreamId: String,
    expire: kotlinx.datetime.Instant,
    sessionId: String?,
    queue: StreamJobQueue,
): Boolean {
    streamMetadata.segments.indices.asFlow().parMapUnordered {
        expireSegmentM4s(
            reqStreamId,
            it,
            expire,
        )
    }.flowOn(Dispatchers.IO).collect {}
    expireInitMp4(reqStreamId, expire)
    expireStreamMetadata(reqStreamId, expire)
    sessionId?.let {
        expireSessionStreamJobId(it, expire)
    }

    val allItemsExist =
        streamMetadata.segments.indices.asFlow().parMapUnordered {
            existsSegmentM4s(
                reqStreamId,
                it,
            )
        }.flowOn(Dispatchers.IO).fold(true) { acc, value -> acc && value } &&
            listOf(
                existsStreamMetadata(reqStreamId),
                existsInitMp4(reqStreamId),
                sessionId?.let {
                    existsSessionStreamJobId(sessionId)
                } ?: true,
            ).fold(true) { acc, value -> acc && value }

    if (allItemsExist) {
        queue.notify(streamMetadata.jobId, ClientStreamEvents.Heartbeat)
    } else {
        queue.cancel(streamMetadata.jobId)
    }

    return allItemsExist
}

private fun CoroutineScope.createStream(
    cache: Cache,
    sessionId: String,
    queue: StreamJobQueue,
    id: String,
    serverMediaInfo: ServerMediaInfo,
    req: CreateStreamRequest,
) {
    withLock(cache, "Lock:Session:$sessionId:Stream", 10.seconds, 10.seconds) {
        cache.getSessionStreamMapping(sessionId)?.let { streamMapping ->
            deleteStream(queue, streamMapping, cache, sessionId)
        }

        cache.putStreamMetadata(
            id,
            StreamMetadataWrapper(null),
            Clock.System.now() + 2.minutes,
        )

        val props =
            StreamProps(
                id = id,
                mediaInfo = serverMediaInfo,
                audioStreamName = req.audioTrack,
                videoStreamName = req.videoTrack,
                subtitleStreamName = req.subtitleTrack,
                normalization = req.normalization,
                startOffset = max(0.0, req.startOffset ?: 0.0).seconds,
            )

        val jobId =
            queue.submit(
                props,
            )

        cache.putSessionStreamMapping(
            sessionId,
            StreamMapping(id, jobId),
            Clock.System.now() + 1.hours,
        )
    }
}

private suspend fun deleteStream(
    queue: StreamJobQueue,
    streamMapping: StreamMapping,
    cache: Cache,
    sessionId: String,
) = with(streamMapping) {
    queue.cancel(jobId)
    cache.getStreamMetadata(streamId)?.streamMetadata?.segments?.indices?.forEach {
        cache.delSegmentM4s(streamId, it)
    }
    cache.delInitMp4(streamId)
    cache.delStreamMetadata(streamId)
    cache.delSessionStreamMapping(sessionId)
}
