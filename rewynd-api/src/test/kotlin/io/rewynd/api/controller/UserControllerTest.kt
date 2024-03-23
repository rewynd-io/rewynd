package io.rewynd.api.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
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
import io.mockk.mockkStatic
import io.rewynd.api.ADMIN_USER
import io.rewynd.api.BaseHarness
import io.rewynd.api.SESSION_ID
import io.rewynd.api.plugins.configureSession
import io.rewynd.common.database.Database
import io.rewynd.common.generateSalt
import io.rewynd.common.hashPassword
import io.rewynd.common.model.ServerUser
import io.rewynd.model.ChangePasswordRequest
import io.rewynd.test.InternalGenerators
import io.rewynd.test.UtilGenerators
import io.rewynd.test.list

internal class UserControllerTest : StringSpec({
    "listUsers" {
        Harness().run {
            testCall(
                { listUsers() },
                setup = { setupApp(db) },
            ) {
                status shouldBe HttpStatusCode.OK.value
                body() shouldBe users.map { it.user }
            }
            coVerify {
                db.listUsers()
            }
        }
    }

    "changePassword" {
        Harness().run {
            val mockDb: Database =
                mockk<Database> {}
            coEvery { mockDb.getUser(user.user.username) } returns userWithOldPass
            coEvery { mockDb.upsertUser(any()) } returns true
            coEvery { mockDb.mkSessionStorage() } returns session
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
        private class Harness(
            user: ServerUser = ADMIN_USER,
            sessionId: String = SESSION_ID,
        ) : BaseHarness(user, sessionId) {
            val users by lazy { InternalGenerators.serverUser.list().next() }
            val newPassword by lazy { Arb.string(minSize = 2).next() }
            val oldPassword by lazy { Arb.string(minSize = 2).next() }
            val oldSalt by lazy { UtilGenerators.urlEncodedBase64.next() }
            val newSalt by lazy { UtilGenerators.urlEncodedBase64.next() }
            val oldHashedPass = hashPassword(oldPassword, oldSalt)
            val newHashedPass = hashPassword(newPassword, newSalt)
            val userWithOldPass by lazy { user.copy(hashedPass = oldHashedPass, salt = oldSalt) }
            val userWithNewPass by lazy { user.copy(hashedPass = newHashedPass, salt = newSalt) }

            init {
                coEvery { db.listUsers() } returns users
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
