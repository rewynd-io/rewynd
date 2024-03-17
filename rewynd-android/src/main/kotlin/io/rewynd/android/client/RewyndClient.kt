package io.rewynd.android.client

import android.util.Log
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.engine.okhttp.OkHttpEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import io.rewynd.android.client.cookie.PersistentCookiesStorage
import io.rewynd.client.RewyndClient

fun mkRewyndClient(serverUrl: ServerUrl) =
    RewyndClient(serverUrl.value, httpClientEngine = OkHttpEngine(OkHttpConfig()), httpClientConfig = {
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
    })
