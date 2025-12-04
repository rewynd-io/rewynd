package io.rewynd.common

import io.rewynd.common.model.ServerAudioTrack
import io.rewynd.common.model.ServerScanTask
import io.rewynd.common.model.ServerScheduleInfo
import io.rewynd.common.model.ServerSubtitleTrack
import io.rewynd.common.model.ServerVideoTrack
import io.rewynd.model.ScanTask
import io.rewynd.model.Schedule
import io.rewynd.model.SortOrder
import kotlinx.datetime.serializers.InstantComponentSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

val encoder: Base64.Encoder by lazy { Base64.getUrlEncoder() }
val decoder: Base64.Decoder by lazy { Base64.getUrlDecoder() }
private val factory by lazy { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1") }
private val random by lazy { SecureRandom() }

private const val SALT_SIZE = 256
private const val ITERATION_COUNT = 65536
private const val KEY_LENGTH = 512

fun generateSalt(): String {
    val salt = ByteArray(SALT_SIZE)
    random.nextBytes(salt)
    return encoder.encodeToString(salt)
}

fun hashPassword(
    password: String,
    salt: String,
): String {
    val spec: KeySpec = PBEKeySpec(password.toCharArray(), decoder.decode(salt), ITERATION_COUNT, KEY_LENGTH)
    return encoder.encodeToString(factory.generateSecret(spec).encoded)
}

fun md5(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    return encoder.encodeToString(md.digest(input.toByteArray()))
}

fun Map<String, ServerVideoTrack>.toVideoTracks() = mapValues { it.value.toVideoTrack() }

fun Map<String, ServerAudioTrack>.toAudioTracks() = mapValues { it.value.toAudioTrack() }

fun Map<String, ServerSubtitleTrack>.toSubtitleTracks() = mapValues { it.value.toSubtitleTrack() }

fun <E> Collection<E>.indexed() = mapIndexed { index, e -> index to e }
typealias SerializableInstant =
    @Serializable
    (InstantComponentSerializer::class)
    Instant

fun Schedule.toServerScheduleInfo(): ServerScheduleInfo =
    ServerScheduleInfo(id = id, cronExpression = cronExpression, scanTasks = scanTasks.map { it.toServerScanTask() })

fun ScanTask.toServerScanTask() = ServerScanTask(libraryId = libraryId)

fun ServerScanTask.toScanTask() = ScanTask(libraryId)

fun ServerScheduleInfo.toSchedule() = Schedule(id, cronExpression, scanTasks.map { it.toScanTask() })

private val MICROS_IN_SECOND = 1.seconds.inWholeMicroseconds
val Duration.partialSecondsString: String
    get() = "%.6f".format(partialSeconds)

val Duration.partialSeconds: Double
    get() = inWholeMicroseconds.toDouble() / MICROS_IN_SECOND

@OptIn(ExperimentalSerializationApi::class)
val JSON = Json {
    ignoreUnknownKeys = true
    allowTrailingComma = true
    isLenient = true
}

fun SortOrder.toSql() = when (this) {
    SortOrder.Descending -> org.jetbrains.exposed.sql.SortOrder.DESC
    SortOrder.Ascending -> org.jetbrains.exposed.sql.SortOrder.ASC
}
