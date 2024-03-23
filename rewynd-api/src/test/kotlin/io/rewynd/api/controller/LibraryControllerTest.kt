package io.rewynd.api.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.next
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpMethod
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
import io.rewynd.api.NON_ADMIN_USER
import io.rewynd.api.SESSION_ID
import io.rewynd.api.plugins.configureSession
import io.rewynd.common.cache.queue.ScanJobQueue
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerUser
import io.rewynd.model.DeleteLibrariesRequest
import io.rewynd.model.Library
import io.rewynd.model.ScanLibrariesRequest
import io.rewynd.test.ApiGenerators
import io.rewynd.test.UtilGenerators
import io.rewynd.test.list
import io.rewynd.test.mockJobQueue

internal class LibraryControllerTest : StringSpec({
    "getLibrary" {
        Harness().run {
            coEvery { db.getLibrary(library.name) } returns library
            testCall<Any?>(
                "/api/lib/get/${library.name}",
                method = HttpMethod.Get,
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK
                body<Library>() shouldBe library
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
            testCall<Any?>(
                "/api/lib/list",
                method = HttpMethod.Get,
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK
                body<List<Library>>() shouldBe libraries
            }

            coVerify {
                db.listLibraries()
            }
        }
    }

    "deleteLibraries - success" {
        val libraryIds = UtilGenerators.string.list().next()

        Harness().run {
            coEvery { db.deleteLibrary(any()) } returns true
            testCall(
                "/api/lib/delete",
                request = DeleteLibrariesRequest(libraries = libraryIds),
                setup = {
                    setupApp(db)
                },
            ) {
                status shouldBe HttpStatusCode.OK
            }

            coVerify {
                libraryIds.forEach {
                    db.deleteLibrary(it)
                }
            }
        }
    }

    "deleteLibraries - forbidden" {
        Harness(NON_ADMIN_USER).run {
            coEvery { db.upsertLibrary(library) } returns true
            testCall<Any?>(
                "/api/lib/delete",
                request = DeleteLibrariesRequest(libraries = UtilGenerators.string.list().next()),
                setup = {
                    setupApp(db)
                },
            ) {
                status shouldBe HttpStatusCode.Forbidden
            }

            coVerify(inverse = true) {
                db.deleteLibrary(any())
            }
        }
    }

    "createLibrary - success" {
        Harness().run {
            coEvery { db.upsertLibrary(library) } returns true
            testCall<Any?>(
                "/api/lib/create",
                request = library,
                setup = {
                    setupApp(db, queue)
                },
            ) {
                status shouldBe HttpStatusCode.OK
            }

            coVerify {
                db.upsertLibrary(library)
                queue.submit(library)
            }
        }
    }

    "createLibrary - forbidden" {
        Harness(NON_ADMIN_USER).run {
            testCall<Any?>(
                "/api/lib/create",
                request = library,
                setup = {
                    setupApp(db, queue)
                },
            ) {
                status shouldBe HttpStatusCode.Forbidden
            }

            coVerify(inverse = true) {
                db.upsertLibrary(any())
                queue.submit(any())
            }
        }
    }

    "scanLibrary - success" {
        Harness().run {
            testCall<Any?>(
                "/api/lib/scan",
                request = ScanLibrariesRequest(listOf(library.name)),
                setup = {
                    setupApp(db, queue)
                },
            ) {
                status shouldBe HttpStatusCode.OK
            }

            coVerify {
                db.getLibrary(any())
                queue.submit(any())
            }
        }
    }

    "scanLibrary - Forbidden" {
        Harness(NON_ADMIN_USER).run {
            testCall<Any?>(
                "/api/lib/scan",
                request = ScanLibrariesRequest(listOf(library.name)),
                setup = {
                    setupApp(db, queue)
                },
            ) {
                status shouldBe HttpStatusCode.Forbidden
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
            val queue: ScanJobQueue = mockJobQueue()
            val library = ApiGenerators.library.next()

            init {
                coEvery { db.getLibrary(library.name) } returns library
            }
        }

        private fun ApplicationTestBuilder.setupApp(
            db: Database,
            scanJobQueue: ScanJobQueue = mockJobQueue(),
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
