package io.rewynd.common.cache.queue

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class MemoryJobQueue<Request, Response, ClientEventPayload, WorkerEventPayload>(
    private val serializeResponse: (Response) -> String,
    private val serializeClientEventPayload: (ClientEventPayload) -> String,
    private val serializeWorkerEventPayload: (WorkerEventPayload) -> String,
    private val deserializeClientEventPayload: (String) -> ClientEventPayload,
) :
    JobQueue<Request, Response, ClientEventPayload, WorkerEventPayload> {
    data class JobResources<Request>(
        val clientChannel: Channel<ClientEvent>,
        val workerChannel: Channel<WorkerEvent>,
        val req: Request,
    )

    private val internalQueue = Channel<JobId>()
    private val jobMap = ConcurrentHashMap<JobId, JobResources<Request>>()

    override suspend fun submit(req: Request): JobId {
        val jobId = JobId()
        val jobReq = JobResources(Channel(), Channel(), req)
        jobMap[jobId] = jobReq
        internalQueue.send(jobId)
        return jobId
    }

    override suspend fun register(
        handler: JobHandler<Request, Response, ClientEventPayload, WorkerEventPayload>,
        scope: CoroutineScope,
    ): Job =
        scope.launch {
            internalQueue.receiveAsFlow().collect { jobId ->
                val resources = jobMap[jobId]
                if (resources != null) {
                    val payloadChannel = Channel<ClientEventPayload>()
                    val job =
                        launch {
                            try {
                                val res =
                                    this.handler(
                                        JobContext(
                                            resources.req,
                                            payloadChannel.consumeAsFlow(),
                                            {
                                                resources.workerChannel.send(
                                                    WorkerEvent.Event(
                                                        serializeWorkerEventPayload(
                                                            it
                                                        )
                                                    )
                                                )
                                            },
                                            jobId,
                                        ),
                                    )
                                resources.workerChannel.send(
                                    WorkerEvent.Success(
                                        serializeResponse(res),
                                    ),
                                )
                            } catch (e: Exception) {
                                resources.workerChannel.send(WorkerEvent.Fail(e.localizedMessage))
                            }
                        }

                    val consumerJob =
                        launch {
                            resources.clientChannel.receiveAsFlow().collect {
                                when (it) {
                                    is ClientEvent.Cancel -> job.cancel()
                                    is ClientEvent.Event -> payloadChannel.send(deserializeClientEventPayload(it.payload))
                                    is ClientEvent.NoOp -> {}
                                }
                            }
                        }
                    try {
                        job.join()
                    } catch (e: CancellationException) {
                        log.info { "Cancelled job ${jobId.value}" }
                    } finally {
                        job.cancel()
                        consumerJob.cancel()
                    }
                }
            }
        }


    override suspend fun monitor(jobId: JobId): Flow<WorkerEvent> =
        jobMap[jobId]?.workerChannel?.consumeAsFlow() ?: emptyFlow()

    override suspend fun cancel(jobId: JobId) {
        jobMap[jobId]?.clientChannel?.send(ClientEvent.Cancel)
    }

    override suspend fun delete(jobId: JobId) {
        jobMap.remove(jobId)
    }

    override fun close() {}

    override suspend fun notify(
        jobId: JobId,
        event: ClientEventPayload,
    ) {
        jobMap[jobId]?.clientChannel?.send(ClientEvent.Event(serializeClientEventPayload(event)))
    }

    companion object {
        private val log by lazy { KotlinLogging.logger {} }
    }
}
