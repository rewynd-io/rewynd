package io.rewynd.common.redis

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.XReadArgs
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.cluster.api.coroutines.RedisClusterCoroutinesCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalLettuceCoroutinesApi::class)
fun <K : Any, V : Any> RedisCoroutinesCommands<K, V>.blpopFlow(vararg keys: K, interval: Duration = 30.seconds) =
    flow {
        while (true) {
            val popped = this@blpopFlow.blpop(interval.inWholeSeconds, *keys)
            popped?.let { emit(it) }
        }
    }.flowOn(Dispatchers.IO)

@OptIn(ExperimentalLettuceCoroutinesApi::class)
fun <K : Any, V : Any> RedisClusterCoroutinesCommands<K, V>.blpopFlow(vararg keys: K, interval: Duration = 30.seconds) =
    flow {
        while (true) {
            val popped = this@blpopFlow.blpop(interval.inWholeSeconds, *keys)
            popped?.let { emit(it) }
        }
    }.flowOn(Dispatchers.IO)

const val XREAD_BLOCKING_TIME = 100L

@OptIn(ExperimentalLettuceCoroutinesApi::class)
fun <K : Any, V : Any> RedisCoroutinesCommands<K, V>.xreadFlow(vararg keys: K) =
    flow {
        val lastMap: MutableMap<K, String?> = keys.associateWith { k -> null }.toMutableMap()
        while (true) {
            val res =
                this@xreadFlow.xread(
                    XReadArgs().block(XREAD_BLOCKING_TIME),
                    *keys.map {
                        XReadArgs.StreamOffset.from(
                            it,
                            lastMap[it] ?: "0",
                        )
                    }.toTypedArray(),
                )
            res.collect {
                lastMap[it.stream] = it.id
                emit(it.body)
            }
        }
    }

@OptIn(ExperimentalLettuceCoroutinesApi::class)
fun <K : Any, V : Any> RedisClusterCoroutinesCommands<K, V>.xreadFlow(vararg keys: K) =
    flow {
        val lastMap: MutableMap<K, String?> = keys.associateWith { k -> null }.toMutableMap()
        val res =
            this@xreadFlow.xread(
                XReadArgs().block(0),
                *keys.map {
                    XReadArgs.StreamOffset.from(
                        it,
                        lastMap[it] ?: "0",
                    )
                }.toTypedArray(),
            )
        res.collect {
            lastMap[it.stream] = it.id
            emit(it.body)
        }
    }

@OptIn(ExperimentalLettuceCoroutinesApi::class)
suspend fun <K : Any, V : Any> Flow<Map<K, V>>.xwrite(
    redis: RedisCoroutinesCommands<K, V>,
    key: K,
) = this.collect {
    redis.xadd(key, it)
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
suspend fun <K : Any, V : Any> Flow<Map<K, V>>.xwrite(
    redis: RedisClusterCoroutinesCommands<K, V>,
    key: K,
) = this.collect {
    redis.xadd(key, it)
}
