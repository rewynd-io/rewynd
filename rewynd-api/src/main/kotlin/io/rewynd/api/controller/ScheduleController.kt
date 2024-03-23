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
import io.rewynd.common.cache.queue.RefreshScheduleJobQueue
import io.rewynd.common.database.Database
import io.rewynd.common.toSchedule
import io.rewynd.common.toServerScheduleInfo
import io.rewynd.model.DeleteScheduleRequest
import io.rewynd.model.Schedule
import org.quartz.CronExpression

fun Route.scheduleRoutes(
    db: Database,
    refreshScheduleJobQueue: RefreshScheduleJobQueue,
) {
    route("/schedule") {
        route("/list") {
            install(mkAdminAuthZPlugin(db))

            get {
                call.respond(db.listSchedules())
            }
        }

        route("/get") {
            install(mkAdminAuthZPlugin(db))

            get("/{scheduleId}") {
                val scheduleInfo = call.parameters["scheduleId"]?.let { db.getSchedule(it) }
                if (scheduleInfo == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(scheduleInfo.toSchedule())
                }
            }
        }

        route("/create") {
            install(mkAdminAuthZPlugin(db))

            post {
                val req = call.receive<Schedule>()
                if (!CronExpression.isValidExpression(req.cronExpression)) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid cron expression")
                } else {
                    db.upsertSchedule(req.toServerScheduleInfo())
                    refreshScheduleJobQueue.submit(Unit)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        route("/delete") {
            install(mkAdminAuthZPlugin(db))

            post {
                val req = call.receive<DeleteScheduleRequest>()
                val anyDeleted =
                    req.ids.map {
                        db.deleteSchedule(it)
                    }.any()
                if (anyDeleted) {
                    refreshScheduleJobQueue.submit(Unit)
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
