package io.rewynd.api.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.map
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rewynd.api.BaseHarness
import io.rewynd.api.plugins.configureSession
import io.rewynd.api.setIsAdmin
import io.rewynd.common.cache.queue.JobId
import io.rewynd.common.cache.queue.RefreshScheduleJobQueue
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerScheduleInfo
import io.rewynd.common.model.ServerUser
import io.rewynd.common.toSchedule
import io.rewynd.model.DeleteScheduleRequest
import io.rewynd.test.ApiGenerators
import io.rewynd.test.InternalGenerators
import io.rewynd.test.checkAllRun
import io.rewynd.test.list

internal class ScheduleControllerTest : StringSpec({
    "getSchedule" {
        Harness.arb.checkAllRun {
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
        Harness.arb.checkAllRun {

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
        Harness.arb.checkAllRun {
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
        Harness.arb.checkAllRun {
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
            user: ServerUser,
            sessionId: String,
            val schedule: ServerScheduleInfo,
            val schedules: List<ServerScheduleInfo>,
            val jobId: JobId,
        ) : BaseHarness(user, sessionId) {
            val queue: RefreshScheduleJobQueue = mockk {}

            init {
                coEvery { queue.submit(Unit) } returns jobId
                coEvery { db.getSchedule(schedule.id) } returns schedule
                coEvery { db.upsertSchedule(schedule) } returns true
                coEvery { db.listSchedules() } returns schedules
            }

            companion object {
                val arb =
                    arbitrary {
                        Harness(
                            InternalGenerators.serverUser.map { it.setIsAdmin(true) }.bind(),
                            ApiGenerators.sessionId.bind(),
                            InternalGenerators.serverScheduleInfo.bind(),
                            InternalGenerators.serverScheduleInfo.list().bind(),
                            InternalGenerators.jobId.bind(),
                        )
                    }
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
