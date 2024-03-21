package io.rewynd.api.controller

import io.kotest.assertions.inspecting
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.next
import io.ktor.client.call.body
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import io.mockk.coVerify
import io.rewynd.api.ADMIN_USER
import io.rewynd.api.NON_ADMIN_USER
import io.rewynd.api.SESSION_ID
import io.rewynd.api.SESSION_SERIALIZER
import io.rewynd.api.UserSession
import io.rewynd.api.plugins.configureSession
import io.rewynd.common.cache.queue.ScanJobQueue
import io.rewynd.common.database.Database
import io.rewynd.model.DeleteLibrariesRequest
import io.rewynd.model.Library
import io.rewynd.model.ScanLibrariesRequest
import io.rewynd.test.ApiGenerators
import io.rewynd.test.InternalGenerators
import io.rewynd.test.MemorySessionStorage
import io.rewynd.test.UtilGenerators
import io.rewynd.test.list
import io.rewynd.test.mockDatabase
import io.rewynd.test.mockJobQueue
import kotlinx.coroutines.runBlocking
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

internal class LibraryControllerTest : StringSpec({
    "getLibrary" {
        val library = ApiGenerators.library.next()
        val db =
            mockDatabase(getLibraryHandler = {
                it shouldBe LIBRARY_ID
                library
            })

        testApplication {
            setupApp(db)
            inspecting(
                mkClient().get("/api/lib/get/$LIBRARY_ID") {
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.OK
                runBlocking {
                    body<Library>() shouldBe library
                }
            }
        }
    }

    "listLibraries" {
        val libraries = ApiGenerators.library.list().next()
        val db =
            mockDatabase(listLibrariesHandler = {
                libraries
            })
        testApplication {
            setupApp(db)
            inspecting(
                mkClient().get("/api/lib/list") {
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                },
            ) {
                status shouldBe HttpStatusCode.OK
                runBlocking {
                    body<List<Library>>() shouldBe libraries
                }
            }
        }
    }

    "deleteLibraries - success" {
        val libraryIds = UtilGenerators.string.list().next()
        val requestedIds = mutableListOf<String>()
        val session =
            MemorySessionStorage().apply {
                write(
                    SESSION_ID,
                    SESSION_SERIALIZER.serialize(
                        UserSession(
                            SESSION_ID,
                            ADMIN_USER.user.username,
                        ),
                    ),
                )
            }
        val db =
            mockDatabase(
                mkSessionStorageHandler = {
                    session
                },
                getUserHandler = {
                    ADMIN_USER
                },
                deleteLibraryHandler = {
                    requestedIds.add(it)
                    true
                },
            )
        testApplication {
            setupApp(db)
            inspecting(
                mkClient().post("/api/lib/delete") {
                    cookie("RewyndIoSession", SESSION_ID)
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                    setBody(DeleteLibrariesRequest(libraries = libraryIds))
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                },
            ) {
                status shouldBe HttpStatusCode.OK
            }
        }

        requestedIds shouldContainExactly libraryIds
    }

    "deleteLibraries - forbidden" {
        val session =
            MemorySessionStorage().apply {
                write(
                    SESSION_ID,
                    SESSION_SERIALIZER.serialize(
                        UserSession(
                            SESSION_ID,
                            NON_ADMIN_USER.user.username,
                        ),
                    ),
                )
            }
        val db =
            mockDatabase(
                mkSessionStorageHandler = { session },
                getUserHandler = {
                    NON_ADMIN_USER
                },
            )
        testApplication {
            setupApp(db)
            inspecting(
                mkClient().post("/api/lib/delete") {
                    cookie("RewyndIoSession", SESSION_ID)
                    url {
                        protocol = URLProtocol.HTTPS
                        setBody(DeleteLibrariesRequest(libraries = UtilGenerators.string.list().next()))
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                    }
                },
            ) {
                status shouldBe HttpStatusCode.Forbidden
            }
        }
        coVerify(inverse = true) { db.deleteLibrary(any()) }
    }

    "createLibrary - success" {
        val library = ApiGenerators.library.next()
        val session =
            MemorySessionStorage().apply {
                write(
                    SESSION_ID,
                    SESSION_SERIALIZER.serialize(
                        UserSession(
                            SESSION_ID,
                            ADMIN_USER.user.username,
                        ),
                    ),
                )
            }
        val db =
            mockDatabase(
                mkSessionStorageHandler = {
                    session
                },
                getUserHandler = {
                    ADMIN_USER
                },
                upsertLibraryHandler = {
                    it shouldBe library
                    true
                },
            )
        testApplication {
            setupApp(db)
            inspecting(
                mkClient().post("/api/lib/create") {
                    cookie("RewyndIoSession", SESSION_ID)
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                    setBody(library)
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                },
            ) {
                status shouldBe HttpStatusCode.OK
            }
        }
    }

    "createLibrary - forbidden" {
        val session =
            MemorySessionStorage().apply {
                write(
                    SESSION_ID,
                    SESSION_SERIALIZER.serialize(
                        UserSession(
                            SESSION_ID,
                            NON_ADMIN_USER.user.username,
                        ),
                    ),
                )
            }
        val db =
            mockDatabase(
                mkSessionStorageHandler = { session },
                getUserHandler = {
                    NON_ADMIN_USER
                },
            )
        testApplication {
            setupApp(db)
            inspecting(
                mkClient().post("/api/lib/create") {
                    cookie("RewyndIoSession", SESSION_ID)
                    url {
                        protocol = URLProtocol.HTTPS
                        setBody(ApiGenerators.library.next())
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                    }
                },
            ) {
                status shouldBe HttpStatusCode.Forbidden
            }
        }
        coVerify(inverse = true) { db.upsertLibrary(any()) }
    }

    "scanLibrary - success" {
        val library = ApiGenerators.library.next()

        val session =
            MemorySessionStorage().apply {
                write(
                    SESSION_ID,
                    SESSION_SERIALIZER.serialize(
                        UserSession(
                            SESSION_ID,
                            ADMIN_USER.user.username,
                        ),
                    ),
                )
            }
        val db =
            mockDatabase(
                mkSessionStorageHandler = {
                    session
                },
                getUserHandler = {
                    ADMIN_USER
                },
                getLibraryHandler = {
                    it shouldBe library.name
                    library
                },
            )
        val queue: ScanJobQueue =
            mockJobQueue(
                submitHandler = {
                    it shouldBe library
                    InternalGenerators.jobId.next()
                },
            )
        testApplication {
            setupApp(db, queue)
            inspecting(
                mkClient().post("/api/lib/scan") {
                    cookie("RewyndIoSession", SESSION_ID)
                    url {
                        protocol = URLProtocol.HTTPS
                    }
                    setBody(ScanLibrariesRequest(listOf(library.name)))
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                },
            ) {
                status shouldBe HttpStatusCode.OK
            }
        }
    }

    "scanLibrary - forbidden" {
        val session =
            MemorySessionStorage().apply {
                write(
                    SESSION_ID,
                    SESSION_SERIALIZER.serialize(
                        UserSession(
                            SESSION_ID,
                            NON_ADMIN_USER.user.username,
                        ),
                    ),
                )
            }
        val db =
            mockDatabase(
                mkSessionStorageHandler = { session },
                getUserHandler = {
                    NON_ADMIN_USER
                },
            )
        val queue: ScanJobQueue = mockJobQueue()
        testApplication {
            setupApp(db, queue)
            inspecting(
                mkClient().post("/api/lib/scan") {
                    cookie("RewyndIoSession", SESSION_ID)
                    url {
                        protocol = URLProtocol.HTTPS
                        setBody(ScanLibrariesRequest(UtilGenerators.string.list(1..10).next()))
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                    }
                },
            ) {
                status shouldBe HttpStatusCode.Forbidden
            }
        }
        coVerify(inverse = true) {
            db.getLibrary(any())
            queue.submit(any())
        }
    }
}) {
    companion object {
        private const val LIBRARY_ID = "FooLibraryId"

        private fun ApplicationTestBuilder.mkClient(cookieStorage: CookiesStorage = AcceptAllCookiesStorage()) =
            createClient {
                install(HttpCookies) {
                    storage = cookieStorage
                }
                install(ClientContentNegotiation) {
                    json()
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
