package io.rewynd.common.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.cluster.RedisClusterClient
import io.rewynd.common.JSON
import io.rewynd.common.cache.queue.JobQueue
import io.rewynd.common.config.CacheConfig
import io.rewynd.common.config.fromConfig
import io.rewynd.common.config.uri
import io.rewynd.common.model.StreamMapping
import io.rewynd.common.model.StreamMetadataWrapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import java.io.Closeable
import java.util.Base64
import kotlin.time.Duration

sealed interface Cache {
    fun <Request, Response, ClientEventPayload, WorkerEventPayload> getJobQueue(
        key: String,
        serializeRequest: (Request) -> String,
        serializeResponse: (Response) -> String,
        serializeClientEventPayload: (ClientEventPayload) -> String,
        serializeWorkerEventPayload: (WorkerEventPayload) -> String,
        deserializeRequest: (String) -> Request,
        deserializeResponse: (String) -> Response,
        deserializeClientEventPayload: (String) -> ClientEventPayload,
        deserializeWorkerEventPayload: (String) -> WorkerEventPayload,
        itemExpiration: Duration,
    ): JobQueue<Request, Response, ClientEventPayload, WorkerEventPayload>

    suspend fun getStreamMetadata(id: String): StreamMetadataWrapper? = get("StreamMetadata:$id")?.let {
        JSON.decodeFromString(it)
    }

    suspend fun putStreamMetadata(
        id: String,
        m3u8: StreamMetadataWrapper,
        expiration: Instant,
    ): Unit = put("StreamMetadata:$id", JSON.encodeToString(m3u8), expiration)

    suspend fun delStreamMetadata(id: String): Unit = del("StreamMetadata:$id")

    suspend fun expireStreamMetadata(
        id: String,
        expiration: Instant,
    ): Unit = expire("StreamMetadata:$id", expiration)

    suspend fun existsStreamMetadata(id: String) = exists("StreamMetadata:$id")

    suspend fun getSegmentM4s(
        streamId: String,
        segmentId: Int,
    ): ByteArray? = get("StreamSegmentM4s:$streamId:$segmentId")?.let { decoder.decode(it) }

    suspend fun putSegmentM4s(
        streamId: String,
        segmentId: Int,
        segment: ByteArray,
        expiration: Instant,
    ): Unit = put("StreamSegmentM4s:$streamId:$segmentId", encoder.encodeToString(segment), expiration)

    suspend fun delSegmentM4s(
        streamId: String,
        segmentId: Int,
    ): Unit = del("StreamSegmentM4s:$streamId:$segmentId")

    suspend fun expireSegmentM4s(
        streamId: String,
        segmentId: Int,
        expiration: Instant,
    ): Unit = expire("StreamSegmentM4s:$streamId:$segmentId", expiration)

    suspend fun existsSegmentM4s(
        streamId: String,
        segmentId: Int,
    ) = exists("StreamSegmentM4s:$streamId:$segmentId")

    suspend fun getInitMp4(streamId: String): ByteArray? = get("StreamInitMp4:$streamId")?.let { decoder.decode(it) }

    suspend fun putInitMp4(
        streamId: String,
        initMp4: ByteArray,
        expiration: Instant,
    ): Unit = put("StreamInitMp4:$streamId", encoder.encodeToString(initMp4), expiration)

    suspend fun delInitMp4(streamId: String): Unit = del("StreamInitMp4:$streamId")

    suspend fun expireInitMp4(
        streamId: String,
        expiration: Instant,
    ): Unit = expire("StreamInitMp4:$streamId", expiration)

    suspend fun existsInitMp4(streamId: String) = exists("StreamInitMp4:$streamId")

    suspend fun getSessionStreamMapping(sessionId: String): StreamMapping? =
        get("SessionStreamJobId:$sessionId")?.let { JSON.decodeFromString(it) }

    suspend fun putSessionStreamMapping(
        sessionId: String,
        streamMapping: StreamMapping,
        expiration: Instant,
    ): Unit = put("SessionStreamJobId:$sessionId", JSON.encodeToString(streamMapping), expiration)

    suspend fun delSessionStreamMapping(sessionId: String): Unit = del("SessionStreamJobId:$sessionId")

    suspend fun expireSessionStreamJobId(
        sessionId: String,
        expiration: Instant,
    ): Unit = expire("SessionStreamJobId:$sessionId", expiration)

    suspend fun existsSessionStreamJobId(sessionId: String) = exists("SessionStreamJobId:$sessionId")

    suspend fun getImage(imageId: String): ByteArray? = get("Image:$imageId")?.let { decoder.decode(it) }

    suspend fun putImage(
        imageId: String,
        image: ByteArray,
        expiration: Instant,
    ): Unit = put("Image:$imageId", encoder.encodeToString(image), expiration)

    suspend fun delImage(imageId: String): Unit = del("Image:$imageId")

    suspend fun expireImage(
        imageId: String,
        expiration: Instant,
    ): Unit = expire("Image:$imageId", expiration)

    suspend fun put(
        key: String,
        value: String,
        expiration: Instant,
    ): Unit

    suspend fun get(key: String): String?

    suspend fun del(key: String): Unit

    suspend fun exists(key: String): Boolean

    suspend fun expire(
        key: String,
        expiration: Instant,
    ): Unit

    suspend fun tryAcquire(
        key: String,
        timeout: Duration,
    ): CacheLock?

    suspend fun pub(
        key: String,
        value: String,
    ): Unit

    suspend fun sub(key: String): Flow<String>

    companion object {
        private val decoder by lazy { Base64.getDecoder() }
        private val encoder by lazy { Base64.getEncoder() }

        @OptIn(ExperimentalLettuceCoroutinesApi::class)
        fun fromConfig(config: CacheConfig = CacheConfig.fromConfig()) =
            when (config) {
                is CacheConfig.RedisConfig ->
                    RedisCache(
                        RedisClient.create(
                            config.uri,
                        ),
                    )

                is CacheConfig.RedisClusterConfig ->
                    RedisClusterCache(
                        RedisClusterClient.create(
                            config.uris,
                        ),
                    )

                is CacheConfig.MemoryConfig ->
                    MemoryCache()
            }
    }
}

interface CacheLock : Closeable {
    val validUntil: kotlinx.datetime.Instant
    val timeout: Duration

    fun release(): Unit

    fun extend(newTimeout: Duration? = null): CacheLock?

    override fun close() {
        release()
    }
}

fun <T> CoroutineScope.withLock(
    original: CacheLock,
    block: suspend CoroutineScope.() -> T,
) = runBlocking {
    val res = async(block = block)
    val job =
        launch {
            var lock: CacheLock? = original
            try {
                while (!res.isCompleted && lock != null && lock.isValid()) {
                    if (lock.remaining() > lock.timeout / 2) {
                        delay(lock.remaining() - lock.timeout / 2)
                    } else {
                        try {
                            lock = lock.extend()
                        } catch (e: CancellationException) {
                            lock?.release()
                            throw e
                        } catch (e: Exception) {
                            res.cancel(CancellationException("Failed to extend lock", e))
                            lock?.release()
                        }
                    }
                }
            } finally {
                lock?.release()
            }
        }
    res.await().also {
        job.cancel()
    }
}

fun <T> CoroutineScope.withLock(
    cache: Cache,
    key: String,
    acquireTimeout: Duration,
    leaseTimeout: Duration,
    block: suspend CoroutineScope.() -> T,
) = runBlocking {
    val start = Clock.System.now()
    while (start + acquireTimeout > Clock.System.now()) {
        val lock = cache.tryAcquire(key, leaseTimeout)
        if (lock != null) {
            return@runBlocking withLock(lock, block)
        }
    }
    null
}

fun <T> CoroutineScope.withLock(
    cache: Cache,
    key: String,
    leaseTimeout: Duration,
    block: suspend CoroutineScope.() -> T,
) = runBlocking {
    val lock = cache.tryAcquire(key, leaseTimeout)
    if (lock != null) {
        return@runBlocking withLock(lock, block)
    } else {
        null
    }
}

fun CacheLock.isValid() = this.validUntil > Clock.System.now()

fun CacheLock.remaining() = this.validUntil - Clock.System.now()
