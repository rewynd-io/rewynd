package io.rewynd.worker.scan.show

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import io.rewynd.model.Actor

data class SeasonNfo(
    val title: String?,
    val year: Int?,
    val premiered: String?,
    val releasedate: String?,
    val seasonnumber: Int?,
    @JacksonXmlElementWrapper(useWrapping = false) val actor: List<Actor>?,
)