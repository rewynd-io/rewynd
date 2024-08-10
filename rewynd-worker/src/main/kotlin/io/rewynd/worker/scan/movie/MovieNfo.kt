package io.rewynd.worker.scan.movie

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import io.rewynd.model.Actor

data class MovieNfo(
    val plot: String?,
    val outline: String?,
    val title: String?,
    val originaltitle: String?,
    @JacksonXmlElementWrapper(useWrapping = false) val director: List<String>?,
    @JacksonXmlElementWrapper(useWrapping = false) val writer: List<String>?,
    @JacksonXmlElementWrapper(useWrapping = false) val credits: List<String>?,
    @JacksonXmlElementWrapper(useWrapping = false) val genre: List<String>?,
    @JacksonXmlElementWrapper(useWrapping = false) val studio: List<String>?,
    @JacksonXmlElementWrapper(useWrapping = false) val tag: List<String>?,
    val rating: Double?,
    val year: Int?,
    val mpaa: String?,
    val imdbid: String?,
    val tmdbid: String?,
    val tagline: String?,
    val country: String?,
    @JacksonXmlElementWrapper(useWrapping = false) val actor: List<Actor>?,
    val criticrating: Int?,
    val premiered: String?,
    val releasedate: String?,
)
