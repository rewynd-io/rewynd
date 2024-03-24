package io.rewynd.common.model

import arrow.optics.optics
import io.rewynd.model.User
import kotlinx.serialization.Serializable

@Serializable
@optics
data class ServerUser(val user: User, val hashedPass: String, val salt: String)
