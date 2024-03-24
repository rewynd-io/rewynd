package io.rewynd.api.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.next
import io.ktor.client.call.body
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
import io.rewynd.api.NON_ADMIN_USER
import io.rewynd.api.plugins.configureSession
import io.rewynd.common.cache.queue.JobId
import io.rewynd.common.cache.queue.ScanJobQueue
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerUser
import io.rewynd.model.DeleteLibrariesRequest
import io.rewynd.model.Library
import io.rewynd.model.ScanLibrariesRequest
import io.rewynd.test.ApiGenerators
import io.rewynd.test.InternalGenerators
import io.rewynd.test.UtilGenerators
import io.rewynd.test.checkAllRun
import io.rewynd.test.list

internal class LibraryControllerTest : StringSpec({
    "getLibrary" {
        Harness.arb.checkAllRun {
            coEvery { db.getLibrary(library.name) } returns library
            testCall(
                {
                    getLibrary(library.name)
                },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe library
            }

            coVerify {
                db.getLibrary(library.name)
            }
        }
    }

    "listLibraries" {
        val libraries = ApiGenerators.library.list().next()
        Harness.arb.checkAllRun {
            coEvery { db.listLibraries() } returns libraries
            testCall(
                { listLibraries() },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe libraries
            }

            coVerify {
                db.listLibraries()
            }
        }
    }

    "deleteLibraries - success" {
        Harness.arb.map { it.copy(userParam = ADMIN_USER, sessionIdParam = it.sessionId) }.checkAllRun {
            coEvery { db.deleteLibrary(any()) } returns true
            testCall(
                { deleteLibraries(DeleteLibrariesRequest(libraryIds)) },
                setup = {
                    setupApp(db)
                },
            ) {
                status shouldBe HttpStatusCode.OK.value
                if (libraryIds.isNotEmpty()) {
                    coVerify {
                        libraryIds.forEach {
                            db.deleteLibrary(it)
                        }
                    }
                } else {
                    coVerify(inverse = true) {
                        db.deleteLibrary(any())
                    }
                }
            }
        }
    }

    "deleteLibraries - forbidden" {
        Harness.arb.map { it.copy(userParam = NON_ADMIN_USER, sessionIdParam = it.sessionId) }.checkAllRun {
            coEvery { db.upsertLibrary(library) } returns true
            testCall(
                { deleteLibraries(DeleteLibrariesRequest(libraryIds)) },
                setup = {
                    setupApp(db)
                },
            ) {
                status shouldBe HttpStatusCode.Forbidden.value
            }

            coVerify(inverse = true) {
                db.deleteLibrary(any())
            }
        }
    }

    "createLibrary - success" {
        Harness.arb.map { it.copy(userParam = ADMIN_USER, sessionIdParam = it.sessionId) }.checkAllRun {
            coEvery { queue.submit(library) } returns jobId
            coEvery { db.upsertLibrary(library) } returns true
            testCall(
                { createLibrary(library) },
                setup = {
                    setupApp(db, queue)
                },
            ) {
                status shouldBe HttpStatusCode.OK.value
            }

            coVerify {
                db.upsertLibrary(library)
                queue.submit(library)
            }
        }
    }

    "createLibrary - forbidden" {
        Harness.arb.map { it.copy(userParam = NON_ADMIN_USER, sessionIdParam = it.sessionId) }.checkAllRun {
            testCall(
                { createLibrary(library) },
                setup = {
                    setupApp(db, queue)
                },
            ) {
                status shouldBe HttpStatusCode.Forbidden.value
            }

            coVerify(inverse = true) {
                db.upsertLibrary(any())
                queue.submit(any())
            }
        }
    }

    "scanLibrary - success" {
        Harness.arb.map { it.copy(userParam = ADMIN_USER, sessionIdParam = it.sessionId) }.checkAllRun {
            coEvery { queue.submit(library) } returns jobId
            testCall(
                { scanLibraries(ScanLibrariesRequest(listOf(library.name))) },
                setup = {
                    setupApp(db, queue)
                },
            ) {
                status shouldBe HttpStatusCode.OK.value
            }

            coVerify {
                db.getLibrary(any())
                queue.submit(any())
            }
        }
    }

    "scanLibrary - Forbidden" {
        Harness.arb.map { it.copy(userParam = NON_ADMIN_USER, sessionIdParam = it.sessionId) }.checkAllRun {
            testCall(
                { scanLibraries(ScanLibrariesRequest(listOf(library.name))) },
                setup = {
                    setupApp(db, queue)
                },
            ) {
                status shouldBe HttpStatusCode.Forbidden.value
            }

            coVerify(inverse = true) {
                db.getLibrary(any())
                queue.submit(any())
            }
        }
    }
}) {
    companion object {
        data class Harness(
            val userParam: ServerUser,
            val sessionIdParam: String,
            val library: Library,
            val libraryIds: List<String>,
            val jobId: JobId,
        ) : BaseHarness(userParam, sessionIdParam) {
            val queue: ScanJobQueue = mockk()

            init {
                coEvery { db.getLibrary(library.name) } returns library
            }

            companion object {
                val arb =
                    arbitrary {
                        Harness(
                            userParam = InternalGenerators.serverUser.bind(),
                            sessionIdParam = ApiGenerators.sessionId.bind(),
                            library = ApiGenerators.library.bind(),
                            libraryIds = UtilGenerators.string.list().bind(),
                            jobId = InternalGenerators.jobId.bind(),
                        )
                    }
            }
        }

        private fun ApplicationTestBuilder.setupApp(
            db: Database,
            scanJobQueue: ScanJobQueue = mockk {},
        ) {
            install(ContentNegotiation) {
                json()
            }
            application {
                configureSession(db)
                routing {
                    route("/api") {
                        libRoutes(db, scanJobQueue)
                    }
                }
            }
        }
    }
}
