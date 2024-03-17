package io.rewynd.api.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.rewynd.api.plugins.mkAdminAuthZPlugin
import io.rewynd.common.cache.queue.ScanJobQueue
import io.rewynd.common.database.Database
import io.rewynd.model.DeleteLibrariesRequest
import io.rewynd.model.Library
import io.rewynd.model.ScanLibrariesRequest

fun Route.libRoutes(
    db: Database,
    scanJobQueue: ScanJobQueue,
) {
    get("/lib/list") {
        call.respond(db.listLibraries())
    }
    get("/lib/get/{lib}") {
        val lib = call.parameters["lib"]?.let { db.getLibrary(it) }
        if (lib == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(lib)
        }
    }
    route("/lib/delete") {
        install(mkAdminAuthZPlugin(db))
        post {
            call.receive<DeleteLibrariesRequest>().libraries.forEach { db.deleteLibrary(it) }
            call.respond(HttpStatusCode.OK)
        }
    }

    route("/lib/create") {
        install(mkAdminAuthZPlugin(db))
        post {
            val lib = call.receive<Library>()
            db.upsertLibrary(lib)
            scanJobQueue.submit(lib)
            call.respond(HttpStatusCode.OK)
        }
    }
    route("/lib/scan") {
        install(mkAdminAuthZPlugin(db))
        post {
            call.receive<ScanLibrariesRequest>().libraryIds.forEach {
                db.getLibrary(it)?.let { lib ->
                    scanJobQueue.submit(lib)
                }
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}
