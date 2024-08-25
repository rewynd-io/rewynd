package io.rewynd.android.login

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.rewynd.android.browser.Prefs
import io.rewynd.android.client.ServerUrl
import io.rewynd.android.client.mkRewyndClient
import io.rewynd.model.LoginRequest
import kotlinx.coroutines.launch
import net.kensand.kielbasa.coroutines.coRunCatching

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val loginState = mutableStateOf<LoginState>(LoginState.LoggedOut)

    val serverUrl = mutableStateOf(Prefs.runCatching { serverUrl }.getOrDefault(ServerUrl("https://")))

    val username = mutableStateOf("")
    val password = mutableStateOf("")

    fun verify() {
        loginState.value = LoginState.PendingVerification
        this.viewModelScope.launch {
            loginState.value = mkRewyndClient(serverUrl.value).coRunCatching {
                require(verify().success)
                setLoggedIn()
            }.getOrDefault(LoginState.LoggedOutVerificationFailed)
        }
    }

    fun login() {
        loginState.value = LoginState.PendingLogin
        this.viewModelScope.launch {
            loginState.value = mkRewyndClient(serverUrl.value).coRunCatching {
                require(login(LoginRequest(username.value, password.value)).success)
                setLoggedIn()
            }.getOrDefault(LoginState.LoggedOutVerificationFailed)
        }
    }

    private fun setLoggedIn(): LoginState.LoggedIn {
        Prefs.serverUrl = serverUrl.value
        return LoginState.LoggedIn
    }
}
