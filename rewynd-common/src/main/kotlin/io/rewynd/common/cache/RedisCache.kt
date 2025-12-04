package io.rewynd.common.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.rewynd.common.cache.queue.JobQueue
import io.rewynd.common.cache.queue.RedisJobQueue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.toJavaDuration
import kotlin.time.toJavaInstant

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisCache(
    private val client: RedisClient,
    private val conn: RedisCoroutinesCommands<String, String> = client.connect().coroutines(),
) : Cache {
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
        itemExpiration: Duration,
    ): JobQueue<Request, Response, ClientEventPayload, WorkerEventPayload> =
        RedisJobQueue(
            key,
            this.client,
            serializeRequest,
            serializeResponse,
            serializeClientEventPayload,
            serializeWorkerEventPayload,
            deserializeRequest,
            deserializeClientEventPayload,
            itemExpiration,
        )

    override suspend fun put(
        key: String,
        value: String,
        expiration: Instant,
    ) {
        conn.psetex(key, expiration.minus(Clock.System.now()).inWholeMilliseconds, value)
    }

    override suspend fun get(key: String): String? = conn.get(key)

    override suspend fun del(key: String) {
        conn.del(key)
    }

    override suspend fun exists(key: String): Boolean = conn.exists(key) == 1L

    override suspend fun expire(
        key: String,
        expiration: Instant,
    ) {
        conn.expireat(key, expiration.toJavaInstant())
    }

    internal class RedisCacheLock(
        private val key: String,
        private val conn: RedisCoroutinesCommands<String, String>,
        private val id: String,
        override val timeout: Duration,
        override val validUntil: Instant,
    ) : CacheLock {
        override fun release() =
            runBlocking {
                conn.eval<Long>(
                    """
                    if redis.call("get", KEYS[1]) == ARGV[1] then
                        return redis.call("del", KEYS[1])
                    else
                        return 0
                    end
                    """.trimIndent(),
                    ScriptOutputType.INTEGER,
                    arrayOf(key),
                    id,
                )

                Unit
            }

        override fun extend(newTimeout: Duration?): CacheLock? =
            runBlocking {
                val nonNullTimeout = newTimeout ?: timeout
                val start = Clock.System.now()
                val setCount =
                    (
                        conn.eval<Long>(
                            """
                            if redis.call("get", KEYS[1]) == ARGV[1] then
                                return redis.call("expireat", KEYS[1], ARGV[2])
                            else
                                return 0
                            end
                            """.trimIndent(),
                            ScriptOutputType.INTEGER,
                            arrayOf(key),
                            id,
                            (start + nonNullTimeout).epochSeconds.toString(),
                        ) ?: 0L
                        ).toLong()
                if (setCount > 0L) {
                    RedisCacheLock(key, conn, id, nonNullTimeout, start + nonNullTimeout)
                } else {
                    release()
                    null
                }
            }
    }

    override suspend fun tryAcquire(
        key: String,
        timeout: Duration,
    ): CacheLock? {
        val id = UUID.randomUUID().toString()
        val start = Clock.System.now()
        return if (conn.set(
                key,
                id,
                SetArgs.Builder.px(timeout.toJavaDuration()).nx(),
            ) == "OK"
        ) {
            RedisCacheLock(key, conn, id, timeout, start + timeout)
        } else {
            conn.eval<String>(
                """
                if redis.call("get", KEYS[1]) == ARGV[1] then
                    return redis.call("del", KEYS[1])
                else
                    return 0
                end
                """.trimIndent(),
                ScriptOutputType.INTEGER,
                arrayOf(key),
                id,
            )

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
}
