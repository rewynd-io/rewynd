package io.rewynd.common.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.cluster.api.coroutines
import io.lettuce.core.cluster.api.coroutines.RedisClusterCoroutinesCommands
import io.lettuce.core.cluster.api.sync.NodeSelection
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands
import io.lettuce.core.cluster.models.partitions.RedisClusterNode
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.protocol.CommandArgs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.util.UUID
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalLettuceCoroutinesApi::class)
internal class RedisClusterCacheTest : StringSpec({
    "put" {
        val harness = setup()

        harness.cache.put(KEY, VALUE_1, expiration)

        coVerify {
            harness.coroutines.psetex(KEY, expireDur.inWholeMilliseconds, VALUE_1)
        }
    }

    "get" {
        val harness = setup()

        harness.cache.get(KEY) shouldBe VALUE_2

        coVerify {
            harness.coroutines.get(KEY)
        }
    }

    "del" {
        val harness = setup()

        harness.cache.del(KEY)

        coVerify {
            harness.coroutines.del(KEY)
        }
    }

    "expire" {
        val harness = setup()

        harness.cache.expire(KEY, expiration)

        coVerify {
            harness.coroutines.expireat(KEY, expiration.toJavaInstant())
        }
    }

    "cache lock release" {
        val harness = setup()
        RedisClusterCache.RedisClusterCacheLock(KEY, ID, harness.client, harness.nodes, expiration, lockTimeout1)
            .release()

        coVerify {
            harness.nodes.forAll {
                it.eval<Long>(
                    """
                    if redis.call("get", KEYS[1]) == ARGV[1] then
                        return redis.call("del", KEYS[1])
                    else
                        return 0
                    end
                    """.trimIndent(),
                    ScriptOutputType.INTEGER,
                    arrayOf(KEY),
                    ID,
                )
            }
        }
    }

    "cache lock extend with same timeout" {
        val harness = setup()

        with(
            RedisClusterCache.RedisClusterCacheLock(KEY, ID, harness.client, harness.nodes, expiration, lockTimeout1)
                .extend(),
        ) {
            shouldNotBeNull()
            shouldBeInstanceOf<RedisClusterCache.RedisClusterCacheLock>()
            timeout shouldBe lockTimeout1
            validUntil shouldBe now + lockTimeout1
        }

        coVerify {
            harness.nodes.forAll {
                it.eval<Long>(
                    """
                    if redis.call("get", KEYS[1]) == ARGV[1] then
                        return redis.call("pexpireat", KEYS[1], ARGV[2])
                    else
                        return 0
                    end
                    """.trimIndent(),
                    ScriptOutputType.INTEGER,
                    arrayOf(KEY),
                    ID,
                    (now + lockTimeout1).toEpochMilliseconds().toString(),
                )
            }
        }
    }

    "cache lock extend with new timeout" {
        val harness = setup()

        with(
            RedisClusterCache.RedisClusterCacheLock(KEY, ID, harness.client, harness.nodes, expiration, lockTimeout1)
                .extend(lockTimeout2),
        ) {
            shouldNotBeNull()
            shouldBeInstanceOf<RedisClusterCache.RedisClusterCacheLock>()
            timeout shouldBe lockTimeout2
            validUntil shouldBe now + lockTimeout2
        }

        coVerify {
            harness.nodes.forAll {
                it.eval<Long>(
                    """
                    if redis.call("get", KEYS[1]) == ARGV[1] then
                        return redis.call("pexpireat", KEYS[1], ARGV[2])
                    else
                        return 0
                    end
                    """.trimIndent(),
                    ScriptOutputType.INTEGER,
                    arrayOf(KEY),
                    ID,
                    (now + lockTimeout2).toEpochMilliseconds().toString(),
                )
            }
        }
    }

    "RedisClusterCacheLock extend failure" {
        val harness = setup(nodeSetups = (0 until 5).map { NodeSetup(nodeEvalHandler = { _, _, _, _ -> 0L }) })

        RedisClusterCache.RedisClusterCacheLock(KEY, ID, harness.client, harness.nodes, expiration, lockTimeout1)
            .extend(lockTimeout2)
            .shouldBeNull()
    }

    "tryAcquire success" {
        val harness =
            setup(
                nodeSetups =
                (0 until 5).map {
                    NodeSetup(nodeSetHandler = { _, _, setArgs ->
                        // SetArgs does not make it easy to verify the correct args are being sent
                        val cmdArgs = CommandArgs(StringCodec.UTF8)
                        setArgs.build(cmdArgs)
                        cmdArgs.toCommandString() shouldBe "PX ${lockTimeout1.inWholeMilliseconds} NX"
                        OK
                    })
                },
            )

        with(
            harness.cache.tryAcquire(KEY, lockTimeout1),
        ) {
            shouldNotBeNull()
            shouldBeInstanceOf<RedisClusterCache.RedisClusterCacheLock>()
            timeout shouldBe lockTimeout1
            validUntil shouldBe now + lockTimeout1
        }

        coVerify {
            harness.nodes.forEach {
                it.set(
                    KEY,
                    uuid.toString(),
                    any(),
                )
            }
        }
    }

    "tryAcquire failure" {
        val harness =
            setup(
                nodeSetups =
                (
                    (0 until 3).map { NodeSetup(nodeSetHandler = { _, _, _ -> "SomethingElse" }) } +
                        listOf(
                            NodeSetup(),
                            NodeSetup(),
                        )
                    ).shuffled(),
            )

        with(
            harness.cache.tryAcquire(KEY, lockTimeout1),
        ) {
            shouldBeNull()
        }

        coVerify {
            harness.nodes.forAll {
                it.set(
                    KEY,
                    uuid.toString(),
                    any(),
                )

                it.eval<String>(
                    """
                    if redis.call("get", KEYS[1]) == ARGV[1] then
                        return redis.call("del", KEYS[1])
                    else
                        return 0
                    end
                    """.trimIndent(),
                    ScriptOutputType.INTEGER,
                    arrayOf(KEY),
                    uuid.toString(),
                )
            }
        }
    }
}) {
    companion object {
        const val KEY = "foo"
        const val VALUE_1 = "bar"
        const val VALUE_2 = "baz"
        const val ID = "id"
        const val OK = "OK"
        val uuid = UUID.randomUUID()
        val expireDur = 1.hours
        val lockTimeout1 = 5.seconds
        val lockTimeout2 = 30.seconds
        val now = Clock.System.now()
        val expiration: Instant = now + expireDur

        @OptIn(ExperimentalLettuceCoroutinesApi::class)
        fun setup(
            psetexHandler: (String, Long, String) -> String? = { _, _, _ -> VALUE_2 },
            getHandler: (String) -> String? = { VALUE_2 },
            delHandler: (Array<String>) -> Long? = { it.size.toLong() },
            expireHandler: (String, Instant) -> Boolean? = { _, _ -> true },
            nodeSetups: List<NodeSetup> = (0 until 5).map { NodeSetup() },
        ) = run {
            mockkStatic(UUID::randomUUID)
            every { UUID.randomUUID() } returns uuid

            mockkObject(Clock.System)
            every { Clock.System.now() } returns now

            mockkStatic(StatefulRedisConnection<String, String>::coroutines)

            val clusterCoroutines =
                mockk<RedisClusterCoroutinesCommands<String, String>> {
                    coEvery { psetex(any(), any(), any()) } answers {
                        val key = it.invocation.args[0] as String
                        val expire = it.invocation.args[1] as Long
                        val value = it.invocation.args[2] as String
                        psetexHandler(key, expire, value)
                    }
                    coEvery { this@mockk.get(any()) } answers {
                        val key = it.invocation.args[0] as String
                        getHandler(key)
                    }
                    coEvery { this@mockk.del(any()) } answers {
                        val key = it.invocation.args[0] as Array<String>
                        delHandler(key)
                    }
                    coEvery { this@mockk.expireat(any(), any<java.time.Instant>()) } answers {
                        val key = it.invocation.args[0] as String
                        val expire = it.invocation.args[1] as java.time.Instant
                        expireHandler(key, Instant.fromEpochMilliseconds(expire.toEpochMilli()))
                    }
                }

            val nodeSelections =
                mockk<NodeSelection<String, String>> {
                    every { asMap() } returns
                        (0 until 5).associate {
                            mockk<RedisClusterNode> {
                                every { uri } returns RedisURI()
                            } to mockk<RedisCommands<String, String>> {}
                        }
                }
            val clusterCommands =
                mockk<RedisAdvancedClusterCommands<String, String>> {
                    every { upstream() } returns nodeSelections
                }

            val connection =
                mockk<StatefulRedisClusterConnection<String, String>> {
                    every { coroutines() } returns clusterCoroutines
                    every { sync() } returns clusterCommands
                }

            val nodes =
                nodeSetups.map { nodeSetup ->
                    mockk<RedisCoroutinesCommands<String, String>> {
                        coEvery {
                            this@mockk.eval<Long>(
                                any<String>(),
                                any(),
                                any<Array<String>>(),
                                *anyVararg<String>(),
                            )
                        } answers {
                            val script = it.invocation.args[0] as String
                            val type = it.invocation.args[1] as ScriptOutputType
                            val keys = it.invocation.args[2] as Array<String>
                            val values = it.invocation.args[3] as Array<String>
                            nodeSetup.nodeEvalHandler(script, type, keys, values)
                        }

                        coEvery {
                            this@mockk.set(any<String>(), any<String>(), any<SetArgs>())
                        } answers {
                            val key = it.invocation.args[0] as String
                            val value = it.invocation.args[1] as String
                            val setArgs = it.invocation.args[2] as SetArgs
                            nodeSetup.nodeSetHandler(key, value, setArgs)
                        }
                    }
                }

            val clusterClient =
                mockk<RedisClusterClient> {
                    every { connect() } returns connection
                }

            val conn =
                mockk<StatefulRedisConnection<String, String>> {
                    every { coroutines() }.returnsMany(nodes)
                }
            val client =
                mockk<RedisClient> {
                    every { connect() } returns conn
                }

            mockkStatic(RedisClient::class)
            every { RedisClient.create(any<RedisURI>()) } returns client

            Mocks(clusterClient, clusterCoroutines, nodes, RedisClusterCache(clusterClient, clusterCoroutines))
        }

        @OptIn(ExperimentalLettuceCoroutinesApi::class)
        data class Mocks(
            val client: RedisClusterClient,
            val coroutines: RedisClusterCoroutinesCommands<String, String>,
            val nodes: List<RedisCoroutinesCommands<String, String>>,
            val cache: RedisClusterCache,
        )

        data class NodeSetup(
            val nodeSetHandler: (
                String,
                String,
                SetArgs,
            ) -> String? = { _: String, _: String, _: SetArgs -> "OK" },
            val nodeEvalHandler: (
                String,
                ScriptOutputType,
                Array<String>,
                Array<String>,
            ) -> Long? = { _: String, _: ScriptOutputType, _: Array<String>, _: Array<String> -> 1L },
        )
    }
}
