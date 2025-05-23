package io.rewynd.android.client.cookie

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import io.rewynd.android.MILLIS_PER_SECOND
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CookieStorageCookieJar(private val cookiesStorage: CookiesStorage) : CookieJar {
    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        runBlocking {
            cookiesStorage.get(Url(url.toUri())).map {
                with(Cookie.Builder()) {
                    name(it.name)
                    value(it.value)
                    if (it.httpOnly) httpOnly()
                    it.path?.also(this::path)
                    it.expires?.timestamp?.apply(this::expiresAt)
                    if ((it.maxAge ?: 0) > 0) {
                        this.expiresAt(
                            (System.currentTimeMillis() / MILLIS_PER_SECOND) + (it.maxAge ?: 0),
                        )
                    }
                    if (it.secure) secure()
                    it.domain?.also(this::domain)
                    // TODO it.encoding?
                    // TODO it.extensions?
                    this
                }.build()
            }
        }

    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>,
    ) = runBlocking {
        cookies.forEach {
            cookiesStorage.addCookie(
                Url(url.toUri()),
                io.ktor.http.Cookie(
                    name = it.name,
                    value = it.value,
                    encoding = CookieEncoding.RAW,
                    maxAge = 0,
                    expires = GMTDate(it.expiresAt),
                    domain = it.domain,
                    path = it.path,
                    secure = it.secure,
                    httpOnly = it.httpOnly,
                ),
            )
        }
    }
}
