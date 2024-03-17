package io.rewynd.android.login

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.rewynd.android.client.ServerUrl
import io.rewynd.android.client.cookie.PersistentCookiesStorage
import io.rewynd.android.client.mkRewyndClient
import io.rewynd.client.RewyndClient
import io.rewynd.model.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    val prefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(
            application.applicationContext,
        ),
) : AndroidViewModel(application) {
    val loginState: MutableStateFlow<LoginState> =
        MutableStateFlow(
            prefs.getString(
                SERVER_URL,
                null,
            )?.let { LoginState.LoggedOut(ServerUrl(it)) } ?: LoginState.ServerSelect(
                ServerUrl("https://"),
            ),
        )
    private val clientConfig: ((HttpClientConfig<*>) -> Unit) = {
        it.install(ContentNegotiation) {
            json()
        }
        it.install(Logging) {
            logger =
                object : Logger {
                    override fun log(message: String) {
                        Log.v("RewyndClient", message)
                    }
                }
            level = LogLevel.ALL
        }
        it.install(HttpCookies) {
            this.storage = PersistentCookiesStorage.INSTANCE
        }
    }

    fun verify() {
        this.viewModelScope.launch {
            val serverUrl = loginState.value.serverUrl
            loginState.value = LoginState.PendingVerification(serverUrl)
            loginState.value =
                try {
                    val client: RewyndClient = mkRewyndClient(serverUrl)

                    when (client.verify().status) {
                        HttpStatusCode.OK.value -> {
                            setLoggedIn()
                        }
                        else -> LoginState.LoggedOutVerificationFailed(serverUrl)
                    }
                } catch (e: Exception) {
                    Log.i("Login", "Login verification failed")
                    LoginState.LoggedOutVerificationFailed(serverUrl)
                }
        }
    }

    fun login(req: LoginRequest) {
        val serverUrl = loginState.value.serverUrl
        loginState.value = LoginState.PendingLogin((serverUrl))
        Log.i("Login", "Logging in")
        this.viewModelScope.launch {
            loginState.value =
                when (mkRewyndClient(serverUrl).login(req).status) {
                    HttpStatusCode.OK.value -> {
                        setLoggedIn()
                    }
                    else -> LoginState.LoggedOut(serverUrl)
                }
        }
    }

    private fun setLoggedIn(): LoginState.LoggedIn {
        val serverUrl = loginState.value.serverUrl
        prefs.edit().apply {
            putString(SERVER_URL, serverUrl.value)
        }.apply()
        return LoginState.LoggedIn(serverUrl)
    }

    companion object {
        class MainViewModelFactory(
            private val application: Application,
            private val serverUrl: ServerUrl,
        ) :
            ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainViewModel(
                    application,
                ) as T
        }

        const val SERVER_URL = "ServerUrl"
    }
}
