package io.rewynd.worker.scan.show

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import io.rewynd.model.Actor

data class ShowNfo(
    val plot: String?,
    val outline: String?,
    // TODO should be a Date
    val dateadded: String?,
    val title: String?,
    val originaltitle: String?,
    val rating: Number?,
    val year: Number?,
    // TODO could probably be an enum
    val mpaa: String?,
    val imdb_id: String?,
    val tmdbid: String?,
    val tvdbid: String?,
    val tvrageid: String?,
    // TODO should be a Date
    val premiered: String?,
    // TODO should be a Date
    val releasedate: String?,
    // TODO should be a Date
    val enddate: String?,
    val runTime: Number?,
    val genre: String?,
    val studio: String?,
    @JacksonXmlElementWrapper(useWrapping = false) val tag: List<String>?,
    @JacksonXmlElementWrapper(useWrapping = false) val actor: List<Actor>?,
    val status: String?,
)
