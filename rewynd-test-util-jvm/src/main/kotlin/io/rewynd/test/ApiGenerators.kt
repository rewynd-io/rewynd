package io.rewynd.test

import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.asString
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.string
import io.kotest.property.exhaustive.enum
import io.rewynd.model.Actor
import io.rewynd.model.AudioTrack
import io.rewynd.model.Library
import io.rewynd.model.LibraryType
import io.rewynd.model.MediaInfo
import io.rewynd.model.SeasonInfo
import io.rewynd.model.SubtitleTrack
import io.rewynd.model.VideoTrack
import io.rewynd.test.UtilGenerators.double
import io.rewynd.test.UtilGenerators.string

object ApiGenerators {
    val streamId = Arb.string()
    val audioTrack = Arb.bind<AudioTrack>()
    val videoTrack = Arb.bind<VideoTrack>()
    val subtitleTrack = Arb.bind<SubtitleTrack>()
    val mediaInfo =
        arbitrary {
            MediaInfo(
                id = string.bind(),
                libraryId = string.bind(),
                audioTracks = (string to audioTrack).map().bind(),
                videoTracks = (string to videoTrack).map().bind(),
                subtitleTracks = (string to subtitleTrack).map().bind(),
                runTime = double.bind(),
            )
        }
    val actor =
        arbitrary {
            Actor(
                name = string.nullable().bind(),
                role = string.nullable().bind(),
            )
        }
    val seasonInfo =
        arbitrary {
            SeasonInfo(
                id = string.bind(),
                showId = string.bind(),
                seasonNumber = double.bind(),
                libraryId = string.bind(),
                showName = string.bind(),
                year = double.nullable().bind(),
                premiered = string.nullable().bind(),
                releaseDate = string.nullable().bind(),
                folderImageId = string.nullable().bind(),
                actors = actor.list().bind(),
            )
        }

    val library =
        arbitrary {
            Library(
                Codepoint.alphanumeric().bind().asString(), // TODO switch back to string.bind()
                string.list().bind(),
                Exhaustive.enum<LibraryType>().toArb().bind(),
            )
        }
}
