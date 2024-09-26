package io.rewynd.client

import io.ktor.http.HttpStatusCode

class RewyndStatusCodeException(val statusCode: HttpStatusCode, message: String? = null) : RuntimeException(
    "Received status code ${statusCode.value}${
        if (message == null) {
            ""
        } else {
            ": $message"
        }
    }"
)