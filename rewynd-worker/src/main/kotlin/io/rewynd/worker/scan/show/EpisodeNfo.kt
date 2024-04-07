package io.rewynd.worker.scan.show

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import io.rewynd.model.Actor

data class EpisodeNfo(
    val plot: String?,
    val outline: String?,
    val title: String?,
    @JacksonXmlElementWrapper(useWrapping = false) val director: List<String>?,
    @JacksonXmlElementWrapper(useWrapping = false) val writer: List<String>?,
    @JacksonXmlElementWrapper(useWrapping = false) val credits: List<String>?,
    val rating: Double?,
    val year: Int?,
    val runtime: Double?,
    @JacksonXmlElementWrapper(useWrapping = false) val actor: List<Actor>?,
    val episode: Int?,
    val episodenumberend: Int?,
    val season: Int?,
    val aired: String?,
)