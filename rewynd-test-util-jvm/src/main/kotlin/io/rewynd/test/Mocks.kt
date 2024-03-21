package io.rewynd.test

import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.next
import io.mockk.coEvery
import io.mockk.mockk
import io.rewynd.common.cache.Cache
import io.rewynd.common.cache.queue.JobHandler
import io.rewynd.common.cache.queue.JobId
import io.rewynd.common.cache.queue.JobQueue
import io.rewynd.common.cache.queue.WorkerEvent
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerEpisodeInfo
import io.rewynd.common.model.ServerImageInfo
import io.rewynd.common.model.ServerSeasonInfo
import io.rewynd.common.model.ServerShowInfo
import io.rewynd.common.model.ServerUser
import io.rewynd.common.model.SessionStorage
import io.rewynd.model.Library
import io.rewynd.model.ListEpisodesByLastUpdatedOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

fun mockDatabase(
    getUserHandler: (String) -> ServerUser? = { InternalGenerators.serverUser.next() },
    mkSessionStorageHandler: () -> SessionStorage = { MemorySessionStorage() },
    listEpisodesHandler: (String) -> List<ServerEpisodeInfo> = {
        Arb.list(InternalGenerators.serverEpisodeInfo).next()
    },
    listEpisodesByLastUpdatedHandler: (
        Long?,
        ListEpisodesByLastUpdatedOrder,
    ) -> List<ServerEpisodeInfo> = { _, _ -> InternalGenerators.serverEpisodeInfo.list().next() },
    listSeasonsHandler: (String) -> List<ServerSeasonInfo> = { InternalGenerators.serverSeasonInfo.list().next() },
    listShowsHandler: (String) -> List<ServerShowInfo> = { InternalGenerators.serverShowInfo.list().next() },
    listLibrariesHandler: () -> List<Library> = { ApiGenerators.library.list().next() },
    getEpisodeHandler: (String) -> ServerEpisodeInfo? = { InternalGenerators.serverEpisodeInfo.next() },
    getSeasonHandler: (String) -> ServerSeasonInfo? = { InternalGenerators.serverSeasonInfo.next() },
    getShowHandler: (String) -> ServerShowInfo? = { InternalGenerators.serverShowInfo.next() },
    getImageHandler: (String) -> ServerImageInfo? = { InternalGenerators.serverImageInfo.next() },
    getLibraryHandler: (String) -> Library? = { ApiGenerators.library.next() },
    deleteLibraryHandler: (String) -> Boolean = { true },
    upsertLibraryHandler: (Library) -> Boolean = { true },
) = mockk<Database> {
    coEvery { getUser(any()) } answers { getUserHandler(it.invocation.args.first() as String) }
    coEvery { mkSessionStorage() } returns mkSessionStorageHandler()
    coEvery { listEpisodes(any()) } answers { listEpisodesHandler(it.invocation.args.first() as String) }
    coEvery {
        listEpisodesByLastUpdated(any(), any())
    } answers {
        listEpisodesByLastUpdatedHandler(
            it.invocation.args.first() as Long?,
            requireNotNull(it.invocation.args[1]) as ListEpisodesByLastUpdatedOrder,
        )
    }
    coEvery { listSeasons(any()) } answers { listSeasonsHandler(it.invocation.args.first() as String) }
    coEvery { listShows(any()) } answers { listShowsHandler(it.invocation.args.first() as String) }
    coEvery { listLibraries() } answers { listLibrariesHandler() }
    coEvery { getEpisode(any()) } answers { getEpisodeHandler(it.invocation.args.first() as String) }
    coEvery { getSeason(any()) } answers { getSeasonHandler(it.invocation.args.first() as String) }
    coEvery { getShow(any()) } answers { getShowHandler(it.invocation.args.first() as String) }
    coEvery { getImage(any()) } answers { getImageHandler(it.invocation.args.first() as String) }
    coEvery { getLibrary(any()) } answers { getLibraryHandler(it.invocation.args.first() as String) }
    coEvery { deleteLibrary(any()) } answers { deleteLibraryHandler(it.invocation.args.first() as String) }
    coEvery { upsertLibrary(any()) } answers { upsertLibraryHandler(it.invocation.args.first() as Library) }
}

fun mockCache(
    getImageHandler: (String) -> ByteArray? = { UtilGenerators.byteArray.next() },
    expireImageHandler: (String, Instant) -> Unit = { _, _ -> },
) = mockk<Cache> {
    coEvery { getImage(any()) } answers { getImageHandler(it.invocation.args.first() as String) }
    coEvery { expireImage(any(), any()) } answers {
        expireImageHandler(
            it.invocation.args.first() as String,
            requireNotNull(it.invocation.args[1]) as Instant,
        )
    }
}

inline fun <reified Request, Response, reified ClientEventPayload, WorkerEventPayload> mockJobQueue(
    crossinline submitHandler: (Request) -> JobId = { InternalGenerators.jobId.next() },
    crossinline monitorHandler: (JobId) -> Flow<WorkerEvent> = { emptyFlow() },
    crossinline cancelHandler: (JobId) -> Unit = { },
    crossinline deleteHandler: (JobId) -> Unit = { },
    crossinline notifyHandler: (JobId, ClientEventPayload) -> Unit = { _, _ -> },
    crossinline registerHandler: (
        JobHandler<Request, Response, ClientEventPayload, WorkerEventPayload>,
        CoroutineScope,
    ) -> Job = { _, scope -> scope.launch { } },
) = mockk<JobQueue<Request, Response, ClientEventPayload, WorkerEventPayload>> {
    coEvery {
        register(
            any(),
            any(),
        )
    } answers {
        registerHandler(
            it.invocation.args.first() as JobHandler<Request, Response, ClientEventPayload, WorkerEventPayload>,
            it.invocation.args[1]!! as CoroutineScope,
        )
    }

    coEvery { submit(any()) } answers {
        submitHandler(it.invocation.args.first() as Request)
    }

    coEvery { monitor(any()) } answers {
        monitorHandler(it.invocation.args.first() as JobId)
    }
    coEvery { cancel(any()) } answers {
        cancelHandler(it.invocation.args.first() as JobId)
    }
    coEvery { delete(any()) } answers {
        deleteHandler(it.invocation.args.first() as JobId)
    }
    coEvery { notify(any(), any()) } answers {
        notifyHandler(
            it.invocation.args.first() as JobId,
            it.invocation.args[1]!! as ClientEventPayload,
        )
    }
}
