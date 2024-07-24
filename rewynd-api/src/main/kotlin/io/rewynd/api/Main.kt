package io.rewynd.api

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.rewynd.common.cache.Cache
import io.rewynd.common.database.Database
import kotlinx.coroutines.runBlocking

fun main(): Unit =
    runBlocking {
        val cache = Cache.fromConfig()
        val db = Database.fromConfig()
        val apiSettings = ApiSettings.fromConfig()
        db.init()

        runApi(apiSettings, db, cache)
    }

fun runApi(
    apiSettings: ApiSettings,
    db: Database,
    cache: Cache,
) = embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = { module(apiSettings, db, cache) })
    .start(wait = true)
