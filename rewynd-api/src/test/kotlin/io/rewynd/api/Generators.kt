package io.rewynd.api

import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind

object Generators {
    val userSession = Arb.bind<UserSession>()
}
