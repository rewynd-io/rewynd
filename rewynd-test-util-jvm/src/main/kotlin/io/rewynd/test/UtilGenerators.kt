package io.rewynd.test

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.kotlinx.datetime.instant
import kotlin.time.Duration.Companion.nanoseconds

object UtilGenerators {
    private val encoder = java.util.Base64.getEncoder()
    private val urlEncoder = java.util.Base64.getUrlEncoder()
    val duration = arbitrary { Arb.long(0..Long.MAX_VALUE).bind().nanoseconds }
    val string = Arb.string()
    val double = Arb.double()
    val boolean = Arb.boolean()
    val instant = Arb.instant()

    val base64 = Arb.byteArray(Arb.int(4..128), Arb.byte()).map { encoder.encodeToString(it) }
    val urlEncodedBase64 = Arb.byteArray(Arb.int(4..128), Arb.byte()).map { urlEncoder.encodeToString(it) }
}
