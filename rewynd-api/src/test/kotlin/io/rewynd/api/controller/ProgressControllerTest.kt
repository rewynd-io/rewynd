package io.rewynd.api.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.next
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.rewynd.api.ADMIN_USER
import io.rewynd.api.BaseHarness
import io.rewynd.api.SESSION_ID
import io.rewynd.api.plugins.configureSession
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerUser
import io.rewynd.common.model.UserProgress
import io.rewynd.model.ListProgressRequest
import io.rewynd.model.ListProgressResponse
import io.rewynd.test.InternalGenerators
import io.rewynd.test.list

internal class ProgressControllerTest : StringSpec({
    "getUserProgress" {
        Harness().run {
            testCall(
                { getUserProgress(progress.id) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe progress.toProgress()
            }

            coVerify {
                db.getProgress(progress.id, user.user.username)
            }
        }
    }

    "listUserProgress" {
        Harness().run {
            coEvery { db.listRecentProgress(user.user.username, null, any(), any()) } returns progresses

            testCall(
                { listProgress(listReq) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe ListProgressResponse(null, progresses.map(UserProgress::toProgress))
            }

            coVerify {
                db.listRecentProgress(
                    user.user.username,
                    listReq.cursor,
                    0.0,
                    1.0,
                )
            }
        }
    }

    "putUserProgress" {
        Harness().run {
            testCall(
                { putUserProgress(progress.toProgress()) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
            }

            coVerify {
                db.upsertProgress(progress.copy(username = user.user.username))
            }
        }
    }
}) {
    companion object {
        class Harness(
            user: ServerUser = ADMIN_USER,
            sessionId: String = SESSION_ID,
        ) : BaseHarness(user, sessionId) {
            val progress by lazy { InternalGenerators.userProgress.next().copy(username = user.user.username) }
            val progresses by lazy { InternalGenerators.userProgress.list().next() }
            val listReq = ListProgressRequest()

            init {
                coEvery { db.getProgress(progress.id, user.user.username) } returns progress
                coEvery { db.upsertProgress(progress) } returns true
            }
        }

        private fun ApplicationTestBuilder.setupApp(db: Database) {
            install(ContentNegotiation) {
                json()
            }
            application {
                configureSession(db)
                routing {
                    route("/api") {
                        progressRoutes(db)
                    }
                }
            }
        }
    }
}
