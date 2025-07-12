package io.rewynd.worker.scan

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.rewynd.common.md5
import io.rewynd.common.model.FileLocation
import io.rewynd.common.model.ServerImageInfo
import io.rewynd.model.Library
import java.io.File
import kotlin.io.path.Path
import kotlin.time.Clock

inline fun <reified T> XmlMapper.readValue(string: String): T = this.readValue(string, T::class.java)

private val subtitleExtensions = setOf("srt", "wvtt", "vtt")
fun File.isSubtitleFile() = subtitleExtensions.contains(this.extension)

private val imageExtensions = setOf("jpg", "jpeg", "png")
fun File.isImageFile() = imageExtensions.contains(this.extension)

fun File.isNfoFile() = this.extension == "nfo"

fun File.id(lib: Library) = md5("${lib.name}:${this.absolutePath}")

fun File.findMediaImage(lib: Library, suffix: String? = null): ServerImageInfo? =
    (
        Path(this.parent).toFile().walk().maxDepth(2).filter {
            it.name.startsWith(this.nameWithoutExtension) &&
                it.isImageFile() &&
                (suffix == null || it.nameWithoutExtension == (this.nameWithoutExtension + suffix))
        }.firstOrNull()
        )?.let {
        ServerImageInfo(
            location = FileLocation.LocalFile(it.absolutePath),
            size = 0L,
            libraryId = lib.name,
            imageId = it.id(lib),
            lastUpdated = Clock.System.now(),
        )
    }
