package io.rewynd.common.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerScheduleInfo(
    val id: String,
    val cronExpression: String,
    val scanTasks: List<ServerScanTask>,
)
