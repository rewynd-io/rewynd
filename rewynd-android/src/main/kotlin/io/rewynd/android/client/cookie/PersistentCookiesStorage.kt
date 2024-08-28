package io.rewynd.android.client.cookie

import arrow.atomic.Atomic
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import io.rewynd.android.browser.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.minutes

object PersistentCookiesStorage : CookiesStorage {
    private val store: AcceptAllCookiesStorage = AcceptAllCookiesStorage()
    private val urls: ConcurrentSkipListSet<String>

    private var flushJob: Atomic<Job?> = Atomic(null)
    private val scope by lazy { CoroutineScope(EmptyCoroutineContext) }

    init {
        val stored = Prefs.cookies

        urls = ConcurrentSkipListSet(stored.keys.map { it })
        runBlocking {
            stored.entries.flatMap { it.value.map { serializableCookie -> it.key to serializableCookie.toCookie() } }
                .forEach { store.addCookie(Url(it.first), it.second) }
        }
    }

    override suspend fun addCookie(
        requestUrl: Url,
        cookie: Cookie,
    ) {
        log.debug { "Added $cookie for $requestUrl" }
        this.store.addCookie(requestUrl, cookie)
        urls.add(requestUrl.toString())
    }

    override fun close() {
        runBlocking {
            this@PersistentCookiesStorage.flush()
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        log.debug { "Fetching for $requestUrl" }

        return this.store.get(requestUrl)
    }

    fun startFlushing() = flushJob.compareAndSet(
        null,
        scope.launch {
            while (true) {
                flush()
                delay(1.minutes)
            }
        }
    )

    private suspend fun flush() {
        Prefs.cookies = serializeCookies()
    }

    private suspend fun serializeCookies() =
        this.urls.associate {
            it.toString() to
                    this.store.get(Url(it))
                        .map { cookie -> SerializableCookie.fromCookie(cookie) }.toSet()
        }

    val log = KotlinLogging.logger { }

    @kotlinx.serialization.Serializable
    data class SerializableCookie(
        val name: String,
        val value: String,
        val encoding: CookieEncoding = CookieEncoding.URI_ENCODING,
        val maxAge: Int = 0,
        val expires: Long? = null,
        val domain: String? = null,
        val path: String? = null,
        val secure: Boolean = false,
        val httpOnly: Boolean = false,
        val extensions: Map<String, String?> = emptyMap(),
    ) {
        fun toCookie() =
            Cookie(
                name = this.name,
                value = this.value,
                encoding = this.encoding,
                maxAge = this.maxAge,
                expires = GMTDate(this.expires),
                domain = this.domain,
                path = this.path,
                secure = this.secure,
                httpOnly = this.httpOnly,
                extensions = this.extensions,
            )

        companion object {
            fun fromCookie(cookie: Cookie) =
                SerializableCookie(
                    name = cookie.name,
                    value = cookie.value,
                    encoding = cookie.encoding,
                    maxAge = cookie.maxAge,
                    expires = cookie.expires?.timestamp,
                    domain = cookie.domain,
                    path = cookie.path,
                    secure = cookie.secure,
                    httpOnly = cookie.httpOnly,
                    extensions = cookie.extensions,
                )

            const val COOKIES_STORE_PREF = "CookieStore"
        }
    }
}
