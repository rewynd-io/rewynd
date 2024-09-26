package io.rewynd.test

import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.asString
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.exhaustive.cartesian
import io.kotest.property.exhaustive.enum
import io.kotest.property.kotlinx.datetime.date
import io.rewynd.model.Actor
import io.rewynd.model.AudioTrack
import io.rewynd.model.CreateStreamRequest
import io.rewynd.model.CreateUserRequest
import io.rewynd.model.DeleteUsersRequest
import io.rewynd.model.Library
import io.rewynd.model.LibraryType
import io.rewynd.model.ListLibrariesRequest
import io.rewynd.model.ListSchedulesRequest
import io.rewynd.model.ListSeasonsRequest
import io.rewynd.model.ListShowsRequest
import io.rewynd.model.ListUsersRequest
import io.rewynd.model.MediaInfo
import io.rewynd.model.NormalizationMethod
import io.rewynd.model.NormalizationProps
import io.rewynd.model.Progress
import io.rewynd.model.SearchRequest
import io.rewynd.model.SearchResponse
import io.rewynd.model.SearchResult
import io.rewynd.model.SearchResultType
import io.rewynd.model.SeasonInfo
import io.rewynd.model.SortOrder
import io.rewynd.model.SubtitleTrack
import io.rewynd.model.User
import io.rewynd.model.UserPermissions
import io.rewynd.model.VideoTrack
import net.kensand.kielbasa.kotest.property.Generators.boolean
import net.kensand.kielbasa.kotest.property.Generators.double
import net.kensand.kielbasa.kotest.property.Generators.duration
import net.kensand.kielbasa.kotest.property.Generators.instant
import net.kensand.kielbasa.kotest.property.Generators.int
import net.kensand.kielbasa.kotest.property.Generators.localDate
import net.kensand.kielbasa.kotest.property.Generators.string

object ApiGenerators {
    val sessionId = Arb.string(minSize = 1)
    val streamId = Codepoint.alphanumeric().map { it.asString() } // TODO switch back to string.bind()
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

    val showId = Codepoint.alphanumeric().map { it.asString() } // TODO switch back to string.bind()
    val seasonId = Codepoint.alphanumeric().map { it.asString() } // TODO switch back to string.bind()

    val seasonInfo =
        arbitrary {
            SeasonInfo(
                id = seasonId.bind(),
                showId = string.bind(),
                seasonNumber = int.bind(),
                libraryId = string.bind(),
                showName = string.bind(),
                year = int.nullable().bind(),
                premiered = localDate.nullable().bind(),
                releaseDate = localDate.nullable().bind(),
                folderImageId = string.nullable().bind(),
                actors = actor.list().bind(),
            )
        }

    val libraryId = Codepoint.alphanumeric().map { it.asString() } // TODO switch back to string.bind()
    val mediaId = Codepoint.alphanumeric().map { it.asString() } // TODO switch back to string.bind()

    val library =
        arbitrary {
            Library(
                libraryId.bind(),
                string.list().bind(),
                Exhaustive.enum<LibraryType>().toArb().bind(),
            )
        }

    val listShowsRequest = Arb.bind<ListShowsRequest>()

    val listSeasonsRequest = Arb.bind<ListSeasonsRequest>()

    val username = Arb.string(minSize = 1)

    val user = Arb.bind<User>()

    val progress =
        arbitrary {
            Progress(
                string.bind(),
                percent = Arb.double(0.0, 1.0).bind(),
                timestamp = instant.bind(),
            )
        }

    val searchRequest =
        arbitrary {
            SearchRequest(string.bind())
        }

    val searchResult =
        arbitrary {
            SearchResult(
                resultType = Exhaustive.enum<SearchResultType>().toArb().bind(), // TODO switch back to string.bind()
                id = Codepoint.alphanumeric().bind().asString(),
                title = string.bind(),
                description = string.bind(),
                score = Arb.double(0.0, 1.0).bind(),
            )
        }

    val searchResponse =
        arbitrary {
            SearchResponse(searchResult.list().bind())
        }
    val normalizationMethod = Exhaustive.enum<NormalizationMethod>().toArb()

    val normalizationProps =
        arbitrary {
            NormalizationProps(normalizationMethod.bind())
        }

    val createStreamRequest =
        arbitrary {
            CreateStreamRequest(
                library = libraryId.bind(),
                id = mediaId.bind(),
                audioTrack = audioTrack.bind().id,
                videoTrack = videoTrack.bind().id,
                subtitleTrack = subtitleTrack.bind().id,
                startOffset = duration.bind().inWholeMilliseconds.toDouble(),
                normalization = normalizationProps.bind(),
            )
        }

    val createUserRequest =
        arbitrary {
            CreateUserRequest(
                username = username.bind(),
                permissions = UserPermissions(boolean.bind()),
                password = Arb.string(minSize = 2).bind(),
            )
        }

    val deleteUsersRequest =
        arbitrary {
            DeleteUsersRequest(
                username.list().bind(),
            )
        }

    val listSchedulesRequest =
        arbitrary {
            ListSchedulesRequest(cursor = string.nullable().bind())
        }

    val listLibrariesRequest =
        arbitrary {
            ListLibrariesRequest(cursor = string.nullable().bind())
        }

    val listUsersRequest =
        arbitrary {
            ListUsersRequest(cursor = string.nullable().bind())
        }
}
