package io.rewynd.test

import io.kotest.property.Arb
import io.kotest.property.PropertyContext

suspend fun <T> Arb<T>.checkAllRun(block: suspend T.() -> Unit): PropertyContext = io.kotest.property.checkAll(this) { block(it) }
