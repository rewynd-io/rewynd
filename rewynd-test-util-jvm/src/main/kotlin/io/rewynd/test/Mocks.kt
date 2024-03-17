package io.rewynd.test

import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.next
import io.mockk.coEvery
import io.mockk.mockk
import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerEpisodeInfo
import io.rewynd.common.model.ServerSeasonInfo
import io.rewynd.common.model.ServerUser
import io.rewynd.common.model.SessionStorage

fun mockDatabase(
    getUserHandler: (String) -> ServerUser? = { InternalGenerators.serverUser.next() },
    mkSessionStorageHandler: () -> SessionStorage = { MemorySessionStorage() },
    listEpisodesHandler: (String) -> List<ServerEpisodeInfo> = { Arb.list(InternalGenerators.serverEpisodeInfo).next() },
    listSeasonsHandler: (String) -> List<ServerSeasonInfo> = { Arb.list(InternalGenerators.serverSeasonInfo).next() },
) = mockk<Database> {
    coEvery { getUser(any()) } answers { getUserHandler(it.invocation.args.first() as String) }
    coEvery { mkSessionStorage() } returns mkSessionStorageHandler()
    coEvery { listEpisodes(any()) } answers { listEpisodesHandler(it.invocation.args.first() as String) }
    coEvery { listSeasons(any()) } answers { listSeasonsHandler(it.invocation.args.first() as String) }
}
