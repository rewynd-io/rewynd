package io.rewynd.android.browser.paging

import io.ktor.http.HttpStatusCode
import java.lang.RuntimeException

class HttpStatusCodeException(val statusCode: HttpStatusCode) : RuntimeException(
    "Received status code ${statusCode.value}: ${statusCode.description}"
)
