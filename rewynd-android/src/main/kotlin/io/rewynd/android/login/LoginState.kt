package io.rewynd.android.login

import io.rewynd.android.client.ServerUrl

sealed interface LoginState {
    val serverUrl: ServerUrl

    data class ServerSelect(override val serverUrl: ServerUrl) : LoginState

    data class LoggedOut(override val serverUrl: ServerUrl) : LoginState

    data class LoggedOutVerificationFailed(override val serverUrl: ServerUrl) : LoginState

    data class PendingLogin(override val serverUrl: ServerUrl) : LoginState

    data class PendingVerification(override val serverUrl: ServerUrl) : LoginState

    data class LoggedIn(override val serverUrl: ServerUrl) : LoginState
}
