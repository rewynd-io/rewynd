package io.rewynd.api.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.rewynd.api.BaseHarness
import io.rewynd.api.plugins.configureSession
import io.rewynd.api.setIsAdmin
import io.rewynd.common.database.Database
import io.rewynd.common.generateSalt
import io.rewynd.common.hashPassword
import io.rewynd.common.model.ServerUser
import io.rewynd.model.ChangePasswordRequest
import io.rewynd.model.CreateUserRequest
import io.rewynd.model.DeleteUsersRequest
import io.rewynd.model.ListUsersRequest
import io.rewynd.model.ListUsersResponse
import io.rewynd.test.ApiGenerators
import io.rewynd.test.InternalGenerators
import io.rewynd.test.checkAllRun
import io.rewynd.test.list
import net.kensand.kielbasa.kotest.property.Generators

internal class UserControllerTest : StringSpec({
    "listUsers" {
        Harness.arb.map { it.copy(userParam = it.user.setIsAdmin(true)) }.checkAllRun {
            val users = serverUsers.map { it.user }
            testCall(
                { listUsers(listUsersReq) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe ListUsersResponse(users, users.lastOrNull()?.username)
            }
            coVerify {
                db.listUsers(listUsersReq.cursor)
            }
        }
    }

    "createUser" {
        Harness.arb.map { it.copy(userParam = it.user.setIsAdmin(true)) }.checkAllRun {
            coEvery { db.upsertUser(any()) } returns true
            testCall(
                { createUser(createUserRequest) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
            }
            coVerify {
                db.upsertUser(any())
            }
        }
    }

    "deleteUser" {
        Harness.arb.map { it.copy(userParam = it.user.setIsAdmin(true)) }.checkAllRun {
            coEvery { db.deleteUser(any()) } returns true
            testCall(
                { deleteUsers(deleteUsersRequest) },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
            }
            if (deleteUsersRequest.users.isNotEmpty()) {
                coVerify {
                    deleteUsersRequest.users.forEach {
                        db.deleteUser(it)
                    }
                }
            }
        }
    }

    "changePassword" {
        Harness.arb.checkAllRun {
            val mockDb: Database =
                mockk<Database> {}
            coEvery { mockDb.getUser(user.user.username) } returns userWithOldPass
            coEvery { mockDb.upsertUser(any()) } returns true
            coEvery { mockDb.mkSessionStorage() } returns sessionStorage
            mockkStatic(::generateSalt)
            coEvery { generateSalt() } returns newSalt
            testCall(
                {
                    changePassword(
                        ChangePasswordRequest(
                            oldPassword,
                            newPassword,
                        ),
                    )
                },
                setup = { setupApp(mockDb) },
            ) {
                status shouldBe HttpStatusCode.OK.value
            }
            coVerify {
                mockDb.getUser(user.user.username)
                mockDb.upsertUser(userWithNewPass)
            }
        }
    }
}) {
    companion object {
        private data class Harness(
            val userParam: ServerUser,
            val sessionIdParam: String,
            val serverUsers: List<ServerUser>,
            val newPassword: String,
            val oldPassword: String,
            val oldSalt: String,
            val newSalt: String,
            val createUserRequest: CreateUserRequest,
            val deleteUsersRequest: DeleteUsersRequest,
            val listUsersReq: ListUsersRequest,
        ) : BaseHarness(userParam, sessionIdParam) {
            val oldHashedPass: String = hashPassword(oldPassword, oldSalt)
            val newHashedPass: String = hashPassword(newPassword, newSalt)
            val userWithOldPass: ServerUser = user.copy(hashedPass = oldHashedPass, salt = oldSalt)
            val userWithNewPass: ServerUser = user.copy(hashedPass = newHashedPass, salt = newSalt)

            init {
                coEvery { db.listUsers(listUsersReq.cursor) } returns serverUsers
            }

            companion object {
                val arb =
                    arbitrary {
                        val oldPass = Arb.string(minSize = 2).bind()
                        Harness(
                            InternalGenerators.serverUser.bind(),
                            ApiGenerators.sessionId.bind(),
                            InternalGenerators.serverUser.list().bind(),
                            oldPass,
                            Arb.string(minSize = 2).filter { it != oldPass }.bind(),
                            Generators.urlEncodedBase64.bind(),
                            Generators.urlEncodedBase64.bind(),
                            ApiGenerators.createUserRequest.bind(),
                            ApiGenerators.deleteUsersRequest.bind(),
                            ApiGenerators.listUsersRequest.bind(),
                        )
                    }
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
                        userRoutes(db)
                    }
                }
            }
        }
    }
}
