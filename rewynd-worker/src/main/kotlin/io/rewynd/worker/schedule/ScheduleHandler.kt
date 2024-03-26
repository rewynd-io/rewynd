package io.rewynd.worker.schedule

import io.rewynd.common.cache.Cache
import io.rewynd.common.cache.queue.RefreshScheduleJobHandler
import io.rewynd.common.cache.queue.RefreshScheduleJobQueue
import io.rewynd.common.cache.queue.ScanJobQueue
import io.rewynd.common.cache.withLock
import io.rewynd.common.database.Database
import io.rewynd.common.database.listAllSchedules
import io.rewynd.common.model.ServerScanTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.quartz.CronScheduleBuilder
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobExecutionContext
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory
import kotlin.time.Duration.Companion.minutes
import org.quartz.Job as QuartzJob

class ScheduleHandler(
    private val db: Database,
    private val cache: Cache,
    private val scanJobQueue: ScanJobQueue,
    private val refreshScheduleJobQueue: RefreshScheduleJobQueue,
    private val scheduler: Scheduler = StdSchedulerFactory().scheduler,
) {
    class ScanJob : QuartzJob {
        override fun execute(context: JobExecutionContext): Unit =
            runBlocking {
                val cache = context.jobDetail.jobDataMap[CACHE_ID_JOB_DATA_KEY] as Cache
                val db = context.jobDetail.jobDataMap[DATABASE_ID_JOB_DATA_KEY] as Database
                val scanJobQueue: ScanJobQueue =
                    context.jobDetail.jobDataMap[SCAN_JOB_QUEUE_ID_JOB_DATA_KEY] as ScanJobQueue
                val libraryId = context.jobDetail.jobDataMap.getString(LIBRARY_ID_JOB_DATA_KEY)
                val jobId = context.jobDetail.jobDataMap.getString(JOB_ID_JOB_DATA_KEY)
                withLock(cache, "$jobId-Lock", 5.minutes) {
                    db.getLibrary(libraryId)?.let {
                        log.info { "Added scan job for $libraryId to queue" }
                        scanJobQueue.submit(it)
                    }
                }
            }
    }

    private fun refreshSchedules() =
        runBlocking {
            log.info { "Refreshing schedules" }
            scheduler.clear()
            db.listAllSchedules().collect { scheduleInfo ->
                scheduleInfo.scanTasks.forEachIndexed { index, task ->
                    scheduleScanJob(
                        task,
                        CronScheduleBuilder.cronSchedule(scheduleInfo.cronExpression),
                        "${scheduleInfo.id}-Scan-$index",
                    )
                }
            }
        }

    private fun mkRefreshScheduleJobHandler(): RefreshScheduleJobHandler =
        {
            refreshSchedules()
        }

    fun run(coroutineScope: CoroutineScope) =
        coroutineScope.launch {
            withLock(cache, "ScheduleLock", 1.minutes) {
                log.info { "Obtained lock for scheduling" }
                var job: Job? = null
                try {
                    job = refreshScheduleJobQueue.register(mkRefreshScheduleJobHandler(), this)
                    scheduler.start()
                    refreshSchedules()
                    job.join()
                } finally {
                    job?.cancel()
                    scheduler.clear()
                    log.info { "Lost lock for scheduling" }
                }
            }
        }

    private fun scheduleScanJob(
        scanTask: ServerScanTask,
        scheduleBuilder: CronScheduleBuilder,
        id: String,
    ) {
        val jobDataMap =
            JobDataMap().apply {
                put(LIBRARY_ID_JOB_DATA_KEY, scanTask.libraryId)
                put(JOB_ID_JOB_DATA_KEY, id)
                put(DATABASE_ID_JOB_DATA_KEY, db)
                put(CACHE_ID_JOB_DATA_KEY, cache)
                put(SCAN_JOB_QUEUE_ID_JOB_DATA_KEY, scanJobQueue)
            }

        scheduler.scheduleJob(
            JobBuilder.newJob(
                ScanJob::class.java,
            ).withIdentity("$id-Job")
                .usingJobData(jobDataMap)
                .build(),
            TriggerBuilder.newTrigger()
                .withIdentity("$id-Trigger")
                .withSchedule(scheduleBuilder)
                .build(),
        )
    }

    companion object {
        val log = KotlinLogging.logger { }
        const val LIBRARY_ID_JOB_DATA_KEY = "LibraryId"
        const val JOB_ID_JOB_DATA_KEY = "JobId"
        const val CACHE_ID_JOB_DATA_KEY = "CacheId"
        const val DATABASE_ID_JOB_DATA_KEY = "DatabaseId"
        const val SCAN_JOB_QUEUE_ID_JOB_DATA_KEY = "ScanJobQueueId"
    }
}
