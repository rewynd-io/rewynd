package io.rewynd.api

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.property.PropertyTesting

object KotestProjectConfig : AbstractProjectConfig() {
    override suspend fun beforeProject() {
        PropertyTesting.defaultIterationCount = 10
    }
}
