package io.rewynd.worker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.next
import io.rewynd.test.InternalGenerators
import kotlin.time.Duration.Companion.seconds

class UtilTest : StringSpec({
    "ServerVideoTrack.frameTime" {
        InternalGenerators.serverVideoTrack.next().copy(rFrameRate = "3125/125").frameTime shouldBe 0.04.seconds
    }
})
