package io.rewynd.omni

import io.rewynd.api.runApi
import io.rewynd.common.cache.Cache
import io.rewynd.common.config.fromConfig
import io.rewynd.common.database.Database
import io.rewynd.worker.runWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(): Unit =
    runBlocking(Dispatchers.IO) {
        val cache = Cache.fromConfig()
        val db = Database.fromConfig()
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
