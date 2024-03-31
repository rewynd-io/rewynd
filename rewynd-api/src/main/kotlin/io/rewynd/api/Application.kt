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
import kotlinx.coroutines.runBlocking

val config by lazy { ServerConfig.fromConfig() }
val cache by lazy { Cache.fromConfig(config.cache) }
val db by lazy {
    Database.fromConfig(config.database).apply {
        runBlocking { init() }
    }
}
val logger by lazy { KotlinLogging.logger { } }

fun main(): Unit =
    runBlocking {
        embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = Application::module)
            .start(wait = true)
    }

fun Application.module() {
    configureHTTP()
    configureMonitoring()
    configureSerialization()

    configureSession(db, false) // TODO make config for this

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error(cause) { "Error handling ${call.request.httpMethod} ${call.request.path()}" }
            call.respondText(text = "Internal Server Error", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        route("/api") {
            install(mkAuthNPlugin())
            authRoutes(db)
            libRoutes(db, cache.getScanJobQueue())
            showRoutes(db)
            seasonRoutes(db)
            episodeRoutes(db)
            userRoutes(db)
            scheduleRoutes(db, cache.getScheduleRefreshJobQueue())
            imageRoutes(db, cache, cache.getImageJobQueue())
            streamRoutes(db, cache, cache.getStreamJobQueue())
            searchRoutes(cache.getSearchJobQueue())
            progressRoutes(db)
        }

        singlePageApplication {
            useResources = true
            react("")
        }
    }
}
