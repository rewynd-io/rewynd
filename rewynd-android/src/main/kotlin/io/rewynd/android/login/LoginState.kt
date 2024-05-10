package io.rewynd.android.login

sealed interface LoginState {
    data object LoggedOut : LoginState

    data object LoggedOutVerificationFailed : LoginState

    data object PendingLogin : LoginState

    data object PendingVerification : LoginState

    data object LoggedIn : LoginState
}
