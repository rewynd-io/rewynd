package io.rewynd.common.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.cluster.api.coroutines
import io.lettuce.core.cluster.api.coroutines.RedisClusterCoroutinesCommands
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands
import io.rewynd.common.cache.queue.JobQueue
import io.rewynd.common.cache.queue.RedisClusterJobQueue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.util.UUID
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisClusterCache(
    private val client: RedisClusterClient,
    private val conn: RedisClusterCoroutinesCommands<String, String> = client.connect().coroutines(),
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
    ): JobQueue<Request, Response, ClientEventPayload, WorkerEventPayload> =
        RedisClusterJobQueue(
            key,
            this.client,
            serializeRequest,
            serializeResponse,
            serializeClientEventPayload,
            serializeWorkerEventPayload,
            deserializeRequest,
            deserializeResponse,
            deserializeClientEventPayload,
            deserializeWorkerEventPayload,
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

    override suspend fun expire(
        key: String,
        expiration: Instant,
    ) {
        conn.expireat(key, expiration.toJavaInstant())
    }

    override suspend fun exists(key: String): Boolean = conn.exists(key) == 1L

    class RedisClusterCacheLock(
        val key: String,
        val id: String,
        val client: RedisClusterClient,
        val nodes: List<RedisCoroutinesCommands<String, String>>,
        override val validUntil: Instant,
        override val timeout: Duration,
    ) : CacheLock {
        override fun release() =
            runBlocking {
                nodes.forEach {
                    it.eval<String>(
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
                }
            }

        override fun extend(newTimeout: Duration?): CacheLock? =
            runBlocking {
                val nonNullTimeout = newTimeout ?: timeout
                val start = Clock.System.now()
                val setCount =
                    nodes.sumOf {
                        (
                            it.eval<Long>(
                                """
                                if redis.call("get", KEYS[1]) == ARGV[1] then
                                    return redis.call("pexpireat", KEYS[1], ARGV[2])
                                else
                                    return 0
                                end
                                """.trimIndent(),
                                ScriptOutputType.INTEGER,
                                arrayOf(key),
                                id,
                                (start + nonNullTimeout).toEpochMilliseconds().toString(),
                            ) ?: 0L
                        )
                    }
                if (setCount > floor(nodes.size.toDouble() / 2.0).toLong()) {
                    val validUntil = start + nonNullTimeout
                    RedisClusterCacheLock(
                        key,
                        id,
                        client,
                        client.getNodeConnections(nonNullTimeout),
                        validUntil,
                        nonNullTimeout,
                    )
                } else {
                    release()
                    null
                }
            }
    }

    // TODO pull this out into a library - There's no other lettuce implementations that I can find
    override suspend fun tryAcquire(
        key: String,
        timeout: Duration,
    ): CacheLock? {
        val id = UUID.randomUUID().toString()
        val nodes = client.getNodeConnections(timeout)

        val start = Clock.System.now()
        val setCount =
            nodes.sumOf {
                (
                    if (it.set(
                            key,
                            id,
                            SetArgs.Builder.px(timeout.toJavaDuration()).nx(),
                        ) == "OK"
                    ) {
                        1
                    } else {
                        0
                    }
                ) as Int
            }
        return if (setCount > Math.floor(nodes.size.toDouble() / 2.0)) {
            RedisClusterCacheLock(key, id, client, nodes, start + timeout, timeout)
        } else {
            nodes.forEach {
                it.eval<String>(
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
            }
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

fun RedisClusterClient.getNodeConnections(timeout: Duration) =
    (connect().sync() as RedisAdvancedClusterCommands<*, *>).upstream().asMap()
        .map { entry ->
            val uri =
                entry.key.uri.apply {
                    this.timeout =
                        timeout.div(1000).coerceAtLeast(10.milliseconds).toJavaDuration()
                }
            RedisClient.create(uri).connect().coroutines()
        }
