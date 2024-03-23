package io.rewynd.api.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
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
import io.rewynd.api.SESSION_ID
import io.rewynd.api.plugins.configureSession
import io.rewynd.common.cache.queue.ScanJobQueue
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerUser
import io.rewynd.model.DeleteLibrariesRequest
import io.rewynd.model.ScanLibrariesRequest
import io.rewynd.test.ApiGenerators
import io.rewynd.test.InternalGenerators
import io.rewynd.test.UtilGenerators
import io.rewynd.test.list

internal class LibraryControllerTest : StringSpec({
    "getLibrary" {
        Harness().run {
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
        Harness().run {
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
        Harness().run {
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
        Harness(NON_ADMIN_USER).run {
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
        Harness().run {
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
        Harness(NON_ADMIN_USER).run {
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
        Harness().run {
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
        Harness(NON_ADMIN_USER).run {
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
        class Harness(
            user: ServerUser = ADMIN_USER,
            sessionId: String = SESSION_ID,
        ) : BaseHarness(user, sessionId) {
            val queue: ScanJobQueue = mockk()
            val library by lazy { ApiGenerators.library.next() }
            val libraryIds by lazy { UtilGenerators.string.list().next() }
            val jobId by lazy { InternalGenerators.jobId.next() }

            init {
                coEvery { db.getLibrary(library.name) } returns library
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
