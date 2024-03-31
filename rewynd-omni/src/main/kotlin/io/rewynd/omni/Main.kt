package io.rewynd.omni

import com.typesafe.config.ConfigFactory
import io.rewynd.api.runApi
import io.rewynd.common.cache.Cache
import io.rewynd.common.config.CacheConfig
import io.rewynd.common.config.DatabaseConfig
import io.rewynd.common.config.fromConfig
import io.rewynd.common.database.Database
import io.rewynd.worker.runWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(): Unit =
    runBlocking(Dispatchers.IO) {
        val config = ConfigFactory.load("omni")
        val cache = Cache.fromConfig(config = CacheConfig.fromConfig(config))
        println("Made Cache $cache")
        val db = Database.fromConfig(config = DatabaseConfig.fromConfig(config))
        db.init()

        listOf(
            launch(Dispatchers.IO) {
                runApi(db, cache)
            },
            launch(Dispatchers.IO) {
                runWorker(db, cache)
            },
        ).joinAll()
    }
