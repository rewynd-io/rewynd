package io.rewynd.worker

import io.rewynd.common.cache.Cache
import io.rewynd.common.cache.queue.getImageJobQueue
import io.rewynd.common.cache.queue.getScanJobQueue
import io.rewynd.common.cache.queue.getScheduleRefreshJobQueue
import io.rewynd.common.cache.queue.getSearchJobQueue
import io.rewynd.common.cache.queue.getStreamJobQueue
import io.rewynd.common.database.Database
import io.rewynd.worker.image.mkImageJobHandler
import io.rewynd.worker.scan.mkScanJobHandler
import io.rewynd.worker.schedule.ScheduleHandler
import io.rewynd.worker.search.SearchHandler
import io.rewynd.worker.stream.mkStreamJobHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import kotlin.time.Duration.Companion.minutes

val config by lazy { WorkerConfig.fromConfig() }
val cache by lazy { Cache.fromConfig(config.cache) }

private val log by lazy { KotlinLogging.logger { } }

fun main() =
    runBlocking(Dispatchers.IO) {
        val db = Database.fromConfig(config.database)
        db.init()

        val searchHandler = SearchHandler(db)
        val searchUpdateJob =
            launch(Dispatchers.IO) {
                while (true) {
                    searchHandler.updateIndicies()
                    delay(1.minutes)
                }
            }

        val scanQueue = cache.getScanJobQueue()
        val streamQueue = cache.getStreamJobQueue()
        val imageQueue = cache.getImageJobQueue()
        val searchQueue = cache.getSearchJobQueue()
        val scheduleRefreshJobQueue = cache.getScheduleRefreshJobQueue()

        val scanJobHandler = scanQueue.register(mkScanJobHandler(db), this)
        val streamJobHandler = streamQueue.register(mkStreamJobHandler(cache), this)
        val searchJobHandler = searchQueue.register(searchHandler.jobHander, this)
        val imageJobHandlers = (0 until 100).map { imageQueue.register(mkImageJobHandler(cache), this) }
        val scheduleJob = ScheduleHandler(db, cache, scanQueue, scheduleRefreshJobQueue).run(this)

        (
            listOf(
                scanJobHandler,
                streamJobHandler,
                searchJobHandler,
                searchUpdateJob,
                scheduleJob,
            ) + imageJobHandlers
        ).joinAll()
    }
