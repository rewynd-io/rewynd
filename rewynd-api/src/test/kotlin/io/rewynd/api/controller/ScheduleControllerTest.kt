package io.rewynd.api.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.next
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rewynd.api.ADMIN_USER
import io.rewynd.api.BaseHarness
import io.rewynd.api.SESSION_ID
import io.rewynd.api.plugins.configureSession
import io.rewynd.common.cache.queue.RefreshScheduleJobQueue
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerUser
import io.rewynd.common.toSchedule
import io.rewynd.model.DeleteScheduleRequest
import io.rewynd.test.InternalGenerators
import io.rewynd.test.list

internal class ScheduleControllerTest : StringSpec({
    "getSchedule" {
        Harness().run {
            testCall(
                { getSchedule(schedule.id) },
                setup = { setupApp(db, queue) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe schedule.toSchedule()
            }

            coVerify {
                db.getSchedule(schedule.id)
            }
        }
    }

    "listSchedules" {
        Harness().run {

            testCall(
                { listSchedules() },
                setup = { setupApp(db, queue) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe schedules.map { it.toSchedule() }
            }

            coVerify {
                db.listSchedules()
            }
        }
    }

    "createSchedule" {
        Harness().run {
            testCall(
                { createSchedule(schedule.toSchedule()) },
                setup = { setupApp(db, queue) },
            ) {
                status shouldBe HttpStatusCode.OK.value
            }

            coVerify {
                db.upsertSchedule(schedule)
            }
        }
    }

    "deleteSchedule" {
        Harness().run {
            coEvery { db.deleteSchedule(any()) } returns true
            testCall(
                { deleteSchedule(DeleteScheduleRequest(schedules.map { it.id })) },
                setup = { setupApp(db, queue) },
            ) {
                status shouldBe HttpStatusCode.OK.value
            }

            schedules.forEach {
                coVerify {
                    db.deleteSchedule(it.id)
                }
            }
        }
    }
}) {
    companion object {
        class Harness(
            user: ServerUser = ADMIN_USER,
            sessionId: String = SESSION_ID,
        ) : BaseHarness(user, sessionId) {
            val schedule by lazy { InternalGenerators.serverScheduleInfo.next() }
            val schedules by lazy { InternalGenerators.serverScheduleInfo.list().next() }
            val jobId by lazy { InternalGenerators.jobId.next() }
            val queue: RefreshScheduleJobQueue = mockk {}

            init {
                coEvery { queue.submit(Unit) } returns jobId
                coEvery { db.getSchedule(schedule.id) } returns schedule
                coEvery { db.upsertSchedule(schedule) } returns true
                coEvery { db.listSchedules() } returns schedules
            }
        }

        private fun ApplicationTestBuilder.setupApp(
            db: Database,
            queue: RefreshScheduleJobQueue,
        ) {
            install(ContentNegotiation) {
                json()
            }
            application {
                configureSession(db)
                routing {
                    route("/api") {
                        scheduleRoutes(db, queue)
                    }
                }
            }
        }
    }
}
