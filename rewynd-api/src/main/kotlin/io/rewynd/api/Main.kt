package io.rewynd.api

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.react
import io.ktor.server.http.content.singlePageApplication
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.rewynd.api.controller.authRoutes
import io.rewynd.api.controller.episodeRoutes
import io.rewynd.api.controller.imageRoutes
import io.rewynd.api.controller.libRoutes
import io.rewynd.api.controller.progressRoutes
import io.rewynd.api.controller.scheduleRoutes
import io.rewynd.api.controller.searchRoutes
import io.rewynd.api.controller.seasonRoutes
import io.rewynd.api.controller.showRoutes
import io.rewynd.api.controller.streamRoutes
import io.rewynd.api.controller.userRoutes
import io.rewynd.api.plugins.configureHTTP
import io.rewynd.api.plugins.configureMonitoring
import io.rewynd.api.plugins.configureSerialization
import io.rewynd.api.plugins.configureSession
import io.rewynd.api.plugins.mkAuthNPlugin
import io.rewynd.common.cache.Cache
import io.rewynd.common.cache.queue.getImageJobQueue
import io.rewynd.common.cache.queue.getScanJobQueue
import io.rewynd.common.cache.queue.getScheduleRefreshJobQueue
import io.rewynd.common.cache.queue.getSearchJobQueue
import io.rewynd.common.cache.queue.getStreamJobQueue
import io.rewynd.common.database.Database
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


fun main(): Unit =
    runBlocking {
        val cache = Cache.fromConfig()
        val db = Database.fromConfig()
        db.init()

        runApi(db, cache).join()
    }

suspend fun runApi(db: Database, cache: Cache) = coroutineScope {
    launch {
        embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = { module(db, cache) })
            .start(wait = true)
    }
}