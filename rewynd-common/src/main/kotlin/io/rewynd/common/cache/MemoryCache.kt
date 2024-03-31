package io.rewynd.common.cache

import io.rewynd.common.cache.queue.JobQueue
import io.rewynd.common.cache.queue.MemoryJobQueue
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

class MemoryCache : Cache {
    private val map: ExpiringMap<String, String> = ExpiringMap.builder().variableExpiration().build()
    private val lockMap: ExpiringMap<String, String> = ExpiringMap.builder().variableExpiration().build()
    private val jobQueues = ConcurrentHashMap<String, JobQueue<*, *, *, *>>()

    override fun <Request, Response, ClientEventPayload, WorkerEventPayload> getJobQueue(
        key: String,
        serializeRequest: (Request) -> String,
        serializeResponse: (Response) -> String,
        serializeClientEventPayload: (ClientEventPayload) -> String,
        serializeWorkerEventPayload: (WorkerEventPayload) -> String,
        deserializeRequest: (String) -> Request,
        deserializeResponse: (String) -> Response,
        deserializeClientEventPayload: (String) -> ClientEventPayload,
        deserializeWorkerEventPayload: (String) -> WorkerEventPayload,
    ): JobQueue<Request, Response, ClientEventPayload, WorkerEventPayload> =
        jobQueues.computeIfAbsent(key) {
            MemoryJobQueue<Request, Response, ClientEventPayload, WorkerEventPayload>(
                serializeResponse,
                serializeClientEventPayload,
                serializeWorkerEventPayload,
                deserializeClientEventPayload,
            )
        } as JobQueue<Request, Response, ClientEventPayload, WorkerEventPayload>

    override suspend fun put(
        key: String,
        value: String,
        expiration: Instant,
    ) {
        map.put(
            key,
            value,
            ExpirationPolicy.CREATED,
            expiration.expirationMillis(),
            TimeUnit.MILLISECONDS,
        )
    }

    override suspend fun get(key: String): String? = map[key]

    override suspend fun del(key: String) {
        map.remove(key)
    }

    override suspend fun exists(key: String): Boolean = map.containsKey(key)

    override suspend fun expire(
        key: String,
        expiration: Instant,
    ) {
        map.setExpiration(key, expiration.expirationMillis(), TimeUnit.MILLISECONDS)
    }

    override suspend fun tryAcquire(
        key: String,
        timeout: Duration,
    ): CacheLock? {
        val leaseId = UUID.randomUUID().toString()
        val leaseCreated =
            lockMap.computeIfAbsent(key) {
                leaseId
            } == leaseId
        return if (leaseCreated) {
            MemoryCacheLock(key, leaseId, lockMap, timeout)
        } else {
            null
        }
    }

    override suspend fun pub(
        key: String,
        value: String,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun sub(key: String): Flow<String> {
        TODO("Not yet implemented")
    }

    private fun Instant.expirationMillis() = (this - Clock.System.now()).inWholeMilliseconds
}

class MemoryCacheLock(
    private val key: String,
    private val leaseId: String,
    private val lockMap: ExpiringMap<String, String>,
    override val timeout: Duration,
    override val validUntil: Instant = Clock.System.now() + timeout,
) : CacheLock {
    override fun release() {
        lockMap.computeIfPresent(key) { _, value ->
            if (value == this.leaseId) {
                null
            } else {
                value
            }
        }
    }

    override fun extend(newTimeout: Duration?): CacheLock? {
        val nonNullTimeout = newTimeout ?: timeout
        val newLeaseId = UUID.randomUUID().toString()
        val res =
            lockMap.compute(key) { _, v ->
                if (v == leaseId) {
                    newLeaseId
                } else {
                    v
                }
            }
        return if (res == newLeaseId) {
            lockMap.setExpiration(key, nonNullTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            MemoryCacheLock(key, newLeaseId, lockMap, nonNullTimeout)
        } else {
            null
        }
    }
}
