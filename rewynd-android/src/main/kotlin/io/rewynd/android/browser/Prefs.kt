package io.rewynd.android.browser

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import io.rewynd.android.App
import io.rewynd.android.client.ServerUrl
import io.rewynd.android.client.cookie.PersistentCookiesStorage
import io.rewynd.model.Progress
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object Prefs {
    private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(App.application) }
    private const val SERVER_URL = "ServerUrl"
    private const val COOKIES_STORE_PREF = "CookieStore"

    var serverUrl: ServerUrl
        get() = requireNotNull(
            sharedPreferences.getString(
                SERVER_URL,
                null,
            )?.let {
                ServerUrl(
                    it,
                )
            }
        ) { "ServerUrl must not be null" }
        set(value) = sharedPreferences.edit {
            putString(SERVER_URL, value.value)
        }

    var cookies: Map<String, Set<PersistentCookiesStorage.SerializableCookie>>
        get() = sharedPreferences.getString(COOKIES_STORE_PREF, null)?.let {
            Json.decodeFromString<Map<String, HashSet<PersistentCookiesStorage.SerializableCookie>>>(it)
        } ?: emptyMap()
        set(value) = sharedPreferences.edit { putString(COOKIES_STORE_PREF, Json.encodeToString(value)) }

    val Progress.clampedPercent: Double
        get() = if(percent > 0.95) 0.0 else percent
}
