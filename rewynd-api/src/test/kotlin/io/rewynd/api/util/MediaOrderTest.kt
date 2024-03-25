package io.rewynd.api.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rewynd.common.database.Database
import io.rewynd.test.InternalGenerators
import io.rewynd.test.list
import io.rewynd.test.uniqueBy

internal class MediaOrderTest : StringSpec({

    "sort by episode number" {
        checkAll(InternalGenerators.serverEpisodeInfo.list()) {
            val expected = it.mapIndexed { index, ep -> ep.copy(episode = index.toDouble()) }
            expected.shuffled().sort() shouldBe expected
        }
    }

    "sort by name" {
        val alphabet = "abcdefghijklmnopqrstuvwxyz"
        checkAll(InternalGenerators.serverEpisodeInfo.list(0 until 26)) { episodes ->
            val expected =
                episodes.mapIndexed { index, ep -> ep.copy(episode = null, title = alphabet[index].toString()) }
            expected.shuffled().sort() shouldBe expected
        }
    }

    "next episode in season" {
        checkAll(InternalGenerators.serverEpisodeInfo.list(1..10)) { episodes ->
            val uniqueEpisodes = episodes.uniqueBy { it.id }
            val sortedEpisodes = uniqueEpisodes.sort()
            val selected = sortedEpisodes.indices.random()
            val db =
                mockk<Database> {
                    coEvery { listEpisodes(any(), any()) } returnsMany listOf(uniqueEpisodes, emptyList())
                }
            getNextEpisodeInSeason(
                db,
                sortedEpisodes[selected],
            ) shouldBe if (selected == sortedEpisodes.size - 1) null else sortedEpisodes[selected + 1]

            coVerify {
                db.listEpisodes(sortedEpisodes[selected].seasonId)
            }
        }
    }

    "missing next episode in season" {
        checkAll(
            InternalGenerators.serverEpisodeInfo,
            InternalGenerators.serverEpisodeInfo.list(),
        ) { episode, episodes ->
            val db =
                mockk<Database> {
                    coEvery { listEpisodes(any(), any()) } returnsMany
                        listOf(
                            episodes.uniqueBy { it.id }
                                .filter { it.id != episode.id },
                            emptyList(),
                        )
                }
            getNextEpisodeInSeason(
                db,
                episode,
            ).shouldBeNull()

            coVerify {
                db.listEpisodes(episode.seasonId)
            }
        }
    }
})
