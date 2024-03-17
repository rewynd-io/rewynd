package io.rewynd.common.model

import io.rewynd.common.cache.queue.JobId
import kotlinx.serialization.Serializable

@Serializable
data class StreamMapping(val streamId: String, val jobId: JobId)
