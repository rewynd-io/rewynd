package io.rewynd.common.cache.queue

import io.rewynd.common.JSON
import io.rewynd.common.cache.Cache
import io.rewynd.common.model.ClientStreamEvents
import io.rewynd.common.model.SearchProps
import io.rewynd.common.model.ServerImageInfo
import io.rewynd.common.model.StreamProps
import io.rewynd.common.model.WorkerStreamEvents
import io.rewynd.model.Library
import io.rewynd.model.SearchResponse
import kotlinx.serialization.encodeToString
import kotlin.time.Duration.Companion.days

typealias SearchJobHandler = JobHandler<SearchProps, SearchResponse, Unit, Unit>
typealias SearchJobQueue = JobQueue<SearchProps, SearchResponse, Unit, Unit>
typealias ScanJobHandler = JobHandler<Library, Unit, Unit, Unit>
typealias ScanJobQueue = JobQueue<Library, Unit, Unit, Unit>
typealias RefreshScheduleJobHandler = JobHandler<Unit, Unit, Unit, Unit>
typealias RefreshScheduleJobQueue = JobQueue<Unit, Unit, Unit, Unit>
typealias StreamJobHandler = JobHandler<StreamProps, Unit, ClientStreamEvents, WorkerStreamEvents>
typealias StreamJobQueue = JobQueue<StreamProps, Unit, ClientStreamEvents, WorkerStreamEvents>
typealias ImageJobHandler = JobHandler<ServerImageInfo, ByteArray, Unit, Unit>
typealias ImageJobQueue = JobQueue<ServerImageInfo, ByteArray, Unit, Unit>

fun Cache.getSearchJobQueue(): SearchJobQueue =
    getJobQueue(
        "SearchJobQueue",
        { JSON.encodeToString(it) },
        { JSON.encodeToString(it) },
        { JSON.encodeToString(it) },
        { JSON.encodeToString(it) },
        { JSON.decodeFromString(it) },
        { JSON.decodeFromString(it) },
        { JSON.decodeFromString(it) },
        { JSON.decodeFromString(it) },
        1.days,
    )

fun Cache.getScheduleRefreshJobQueue(): RefreshScheduleJobQueue =
    getJobQueue(
        "ScheduleJobQueue",
        { JSON.encodeToString(it) },
        { JSON.encodeToString(it) },
        { JSON.encodeToString(it) },
        { JSON.encodeToString(it) },
        { JSON.decodeFromString(it) },
        { JSON.decodeFromString(it) },
        { JSON.decodeFromString(it) },
        { JSON.decodeFromString(it) },
        1.days,
    )

fun Cache.getStreamJobQueue(): StreamJobQueue =
    getJobQueue(
        "StreamJobQueue",
        { JSON.encodeToString(it) },
        { JSON.encodeToString(it) },
        { JSON.encodeToString(it) },
        { JSON.encodeToString(it) },
        { JSON.decodeFromString(it) },
        { JSON.decodeFromString(it) },
        { JSON.decodeFromString(it) },
        { JSON.decodeFromString(it) },
        1.days,
    )

fun Cache.getScanJobQueue(): ScanJobQueue =
    getJobQueue(
        "ScanJobQueue",
        { JSON.encodeToString(it) },
        { JSON.encodeToString(it) },
        { JSON.encodeToString(it) },
        { JSON.encodeToString(it) },
        { JSON.decodeFromString(it) },
        { JSON.decodeFromString(it) },
        { JSON.decodeFromString(it) },
        { JSON.decodeFromString(it) },
        1.days,
    )

fun Cache.getImageJobQueue(): ImageJobQueue =
    getJobQueue(
        "ImageJobQueue",
        { JSON.encodeToString(it) },
        { JSON.encodeToString(it) },
        { JSON.encodeToString(it) },
        { JSON.encodeToString(it) },
        { JSON.decodeFromString(it) },
        { JSON.decodeFromString(it) },
        { JSON.decodeFromString(it) },
        { JSON.decodeFromString(it) },
        1.days,
    )
