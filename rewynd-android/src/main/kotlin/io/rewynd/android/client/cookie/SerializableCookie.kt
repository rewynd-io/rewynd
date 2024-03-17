package io.rewynd.android.client.cookie

import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.util.date.GMTDate

// suspend fun RewyndClient.parseCookies(cookies: List<SerializableCookie>) = this.client.setCookie()
@kotlinx.serialization.Serializable
data class SerializableCookie(
    val name: String,
    val value: String,
    val encoding: CookieEncoding = CookieEncoding.URI_ENCODING,
    val maxAge: Int = 0,
    val expiresEpochMillis: Long? = null,
    val domain: String? = null,
    val path: String? = null,
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
    val extensions: Map<String, String?> = emptyMap(),
) {
    fun to() =
        Cookie(
            name = this.name,
            value = this.value,
            encoding = this.encoding,
            maxAge = this.maxAge,
            expires = this.expiresEpochMillis?.let { GMTDate(it) },
            domain = this.domain,
            path = this.path,
            secure = this.secure,
            httpOnly = this.httpOnly,
            extensions = this.extensions,
        )

    companion object {
        fun from(cookie: Cookie) =
            SerializableCookie(
                name = cookie.name,
                value = cookie.value,
                encoding = cookie.encoding,
                maxAge = cookie.maxAge,
                expiresEpochMillis = cookie.expires?.timestamp,
                domain = cookie.domain,
                path = cookie.path,
                secure = cookie.secure,
                httpOnly = cookie.httpOnly,
                extensions = cookie.extensions,
            )
    }
}
