package io.rewynd.worker

import io.github.oshai.kotlinlogging.KotlinLogging
import io.rewynd.common.model.ServerVideoTrack
import org.apache.lucene.store.ByteBuffersDirectory
import org.apache.lucene.store.Directory
import org.apache.lucene.store.IOContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

private val log by lazy { KotlinLogging.logger { } }

private val EXEC_TIMEOUT = 5.seconds
fun List<String>.execToString() =
    Runtime
        .getRuntime()
        .exec(this.toTypedArray())
        .let {
            if (it.waitFor(EXEC_TIMEOUT.inWholeSeconds, TimeUnit.SECONDS)) {
                log.error { it.errorReader().readText() }
                val text = it.inputReader().readText()
                log.info { text }
                text
            } else {
                it.destroy()
                throw TimeoutException("Command failed to execute in allotted time: ${this.joinToString(" ")}")
            }
        }

fun Directory.serialize(): ByteArray =
    ByteArrayOutputStream().let { bos ->
        ZipOutputStream(bos).let { writer ->
            listAll().map { fileName ->
                openInput(fileName, IOContext.READONCE).use {
                    val arr = ByteArray(it.length().toInt())
                    val entry = ZipEntry(fileName)
                    it.readBytes(arr, 0, it.length().toInt())
                    writer.putNextEntry(entry)
                    writer.write(arr)
                    writer.closeEntry()
                }
            }
        }
        bos.toByteArray()
    }

fun deserializeDirectory(byteArray: ByteArray) =
    ZipInputStream(byteArray.inputStream()).use { zip ->
        val directory = ByteBuffersDirectory()
        do {
            val entry = zip.nextEntry
            if (entry != null) {
                directory.createOutput(entry.name, IOContext.READONCE).use {
                    val bytes = zip.readAllBytes()
                    it.writeBytes(bytes, bytes.size)
                }
            }
        } while (entry != null)
        directory
    }

fun min(a: Duration, b: Duration) =
    min(a.inWholeNanoseconds, b.inWholeNanoseconds).nanoseconds

fun max(a: Duration, b: Duration) =
    max(a.inWholeNanoseconds, b.inWholeNanoseconds).nanoseconds

val ServerVideoTrack.frameTime
    get() =
        rFrameRate?.split("/")?.takeIf { it.size == 2 }?.let {
            it[0].toDoubleOrNull()?.let { numerator ->
                it[1].toDoubleOrNull()?.let { denominator ->
                    1 / (numerator / denominator)
                }
            }
        }?.seconds
