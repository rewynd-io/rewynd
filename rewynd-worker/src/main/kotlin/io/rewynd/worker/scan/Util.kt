package io.rewynd.worker.scan

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import java.io.File

inline fun <reified T> XmlMapper.readValue(string: String): T = this.readValue(string, T::class.java)

private val subtitleExtensions = setOf("srt", "wvtt", "vtt")
fun File.isSubtitleFile() = subtitleExtensions.contains(this.extension)

private val imageExtensions = setOf("jpg", "jpeg", "png")
fun File.isImageFile() = imageExtensions.contains(this.extension)
