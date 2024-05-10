package io.rewynd.android.login

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.ktor.http.HttpStatusCode
import io.rewynd.android.client.ServerUrl
import io.rewynd.android.client.mkRewyndClient
import io.rewynd.client.RewyndClient
import io.rewynd.model.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val prefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(
            application.applicationContext,
        ),
) : AndroidViewModel(application) {
    val loginState: MutableStateFlow<LoginState> = MutableStateFlow(LoginState.LoggedOut)

    val serverUrl: MutableStateFlow<ServerUrl> =
        MutableStateFlow(
            prefs.getString(SERVER_URL, null)?.let { ServerUrl(it) } ?: ServerUrl("https://"),
        )

    val username: MutableStateFlow<String> = MutableStateFlow("")
    val password: MutableStateFlow<String> = MutableStateFlow("")

    fun verify() {
        loginState.value = LoginState.PendingVerification
        this.viewModelScope.launch {
            loginState.value =
                try {
                    val client: RewyndClient = mkRewyndClient(serverUrl.value)

                    when (client.verify().status) {
                        HttpStatusCode.OK.value -> {
                            setLoggedIn()
                        }

                        else -> LoginState.LoggedOutVerificationFailed
                    }
                } catch (e: Exception) {
                    Log.i("Login", "Login verification failed")
                    LoginState.LoggedOutVerificationFailed
                }
        }
    }

    fun login() {
        loginState.value = LoginState.PendingLogin
        Log.i("Login", "Logging in")
        this.viewModelScope.launch {
            loginState.value =
                try {
                    when (mkRewyndClient(serverUrl.value).login(LoginRequest(username.value, password.value)).status) {
                        HttpStatusCode.OK.value -> {
                            setLoggedIn()
                        }

                        else -> LoginState.LoggedOut
                    }
                } catch (e: Exception) {
                    LoginState.LoggedOutVerificationFailed
                }
        }
    }

    private fun setLoggedIn(): LoginState.LoggedIn {
        prefs.edit().apply {
            putString(SERVER_URL, serverUrl.value.value)
        }.apply()
        return LoginState.LoggedIn
    }

    companion object {
        const val SERVER_URL = "ServerUrl"
    }
}
