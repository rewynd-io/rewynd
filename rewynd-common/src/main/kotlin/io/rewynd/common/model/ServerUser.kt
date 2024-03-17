package io.rewynd.common.model

import io.rewynd.model.User
import kotlinx.serialization.Serializable

@Serializable
data class ServerUser(val user: User, val hashedPass: String, val salt: String)
