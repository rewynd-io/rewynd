package io.rewynd.api.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.compression.minimumSize
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.routing

const val COMPRESSION_PRIORITY = 1.0
const val DEFLATION_PRIORITY = 10.0
const val MINIMUM_COMPRESSION_SIZE = 1024L
const val MAX_RANGE_COUNT = 10

fun Application.configureHTTP() {
    install(IgnoreTrailingSlash)
    install(Compression) {
        gzip {
            priority = COMPRESSION_PRIORITY
        }
        deflate {
            priority = DEFLATION_PRIORITY
            minimumSize(MINIMUM_COMPRESSION_SIZE)
        }
    }
    install(PartialContent) {
        // Maximum number of ranges that will be accepted from a HTTP request.
        // If the HTTP request specifies more ranges, they will all be merged into a single range.
        maxRangeCount = MAX_RANGE_COUNT
    }
    routing {
        swaggerUI(path = "/docs", "src/openapi.yaml")
    }
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }
}
