package io.rewynd.common.cache

import io.kotest.assertions.inspecting
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
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
internal class RedisCacheTest : StringSpec({
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
        RedisCache.RedisCacheLock(KEY, harness.coroutines, ID, lockTimeout1, expiration).release()

        coVerify {
            harness.coroutines.eval<Long>(
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

    "cache lock extend with same timeout" {
        val harness = setup()

        inspecting(
            RedisCache.RedisCacheLock(KEY, harness.coroutines, ID, lockTimeout1, expiration).extend(),
        ) {
            shouldNotBeNull()
            shouldBeInstanceOf<RedisCache.RedisCacheLock>()
            timeout shouldBe lockTimeout1
            validUntil shouldBe now + lockTimeout1
        }

        coVerify {
            harness.coroutines.eval<Long>(
                """
                if redis.call("get", KEYS[1]) == ARGV[1] then
                    return redis.call("expireat", KEYS[1], ARGV[2])
                else
                    return 0
                end
                """.trimIndent(),
                ScriptOutputType.INTEGER,
                arrayOf(KEY),
                ID,
                (now + lockTimeout1).epochSeconds.toString(),
            )
        }
    }

    "cache lock extend with new timeout" {
        val harness = setup()

        inspecting(
            RedisCache.RedisCacheLock(KEY, harness.coroutines, ID, lockTimeout1, expiration).extend(lockTimeout2),
        ) {
            shouldNotBeNull()
            shouldBeInstanceOf<RedisCache.RedisCacheLock>()
            timeout shouldBe lockTimeout2
            validUntil shouldBe now + lockTimeout2
        }

        coVerify {
            harness.coroutines.eval<Long>(
                """
                if redis.call("get", KEYS[1]) == ARGV[1] then
                    return redis.call("expireat", KEYS[1], ARGV[2])
                else
                    return 0
                end
                """.trimIndent(),
                ScriptOutputType.INTEGER,
                arrayOf(KEY),
                ID,
                (now + lockTimeout2).epochSeconds.toString(),
            )
        }
    }

    "RedisCacheLock extend failure" {
        val harness = setup(evalHandler = { _, _, _, _ -> 0L })

        RedisCache.RedisCacheLock(KEY, harness.coroutines, ID, lockTimeout1, expiration)
            .extend(lockTimeout2)
            .shouldBeNull()
    }

    "tryAcquire success" {
        val harness =
            setup(setHandler = { _, _, setArgs ->
                // SetArgs does not make it easy to verify the correct args are being sent
                val cmdArgs = CommandArgs(StringCodec.UTF8)
                setArgs.build(cmdArgs)
                cmdArgs.toCommandString() shouldBe "PX ${lockTimeout1.inWholeMilliseconds} NX"
                OK
            })

        inspecting(
            harness.cache.tryAcquire(KEY, lockTimeout1),
        ) {
            shouldNotBeNull()
            shouldBeInstanceOf<RedisCache.RedisCacheLock>()
            timeout shouldBe lockTimeout1
            validUntil shouldBe now + lockTimeout1
        }

        coVerify {
            harness.coroutines.set(
                KEY,
                uuid.toString(),
                any(),
            )
        }
    }

    "tryAcquire failure" {
        val harness = setup(setHandler = { _, _, _ -> "SomethingElse" })

        inspecting(
            harness.cache.tryAcquire(KEY, lockTimeout1),
        ) {
            shouldBeNull()
        }

        coVerify {
            harness.coroutines.set(
                KEY,
                uuid.toString(),
                any(),
            )

            harness.coroutines.eval<String>(
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
            setHandler: (String, String, SetArgs) -> String? = { _, _, _ -> "OK" },
            getHandler: (String) -> String? = { VALUE_2 },
            delHandler: (Array<String>) -> Long? = { it.size.toLong() },
            expireHandler: (String, Instant) -> Boolean? = { _, _ -> true },
            evalHandler: (String, ScriptOutputType, Array<String>, Array<String>) -> Long? = { _, _, _, _ -> 1 },
        ) = run {
            mockkStatic(UUID::randomUUID)
            every { UUID.randomUUID() } returns uuid

            mockkObject(Clock.System)
            every { Clock.System.now() } returns now

            val coroutines =
                mockk<RedisCoroutinesCommands<String, String>> {
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
                        evalHandler(script, type, keys, values)
                    }

                    coEvery {
                        this@mockk.set(any(), any(), any<SetArgs>())
                    } answers {
                        val key = it.invocation.args[0] as String
                        val value = it.invocation.args[1] as String
                        val setArgs = it.invocation.args[2] as SetArgs
                        setHandler(key, value, setArgs)
                    }
                }

            val connection =
                mockk<StatefulRedisConnection<String, String>> {
                    every { coroutines() } returns coroutines
                }

            val client =
                mockk<RedisClient> {
                    every { connect() } returns connection
                }

            Mocks(client, coroutines, RedisCache(client, coroutines))
        }

        @OptIn(ExperimentalLettuceCoroutinesApi::class)
        data class Mocks(
            val client: RedisClient,
            val coroutines: RedisCoroutinesCommands<String, String>,
            val cache: RedisCache,
        )
    }
}
