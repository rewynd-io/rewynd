package io.rewynd.android.image

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.rewynd.android.browser.getState
import io.rewynd.android.model.ImageId
import io.rewynd.client.RewyndClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Buffer
import okio.FileSystem

class RewyndClientFetcher(private val client: RewyndClient, private val imageId: ImageId) : Fetcher {
    override suspend fun fetch(): FetchResult? = getState(imageId.value, client::getImage).map { retrieved ->
        retrieved?.let {
            val buffer = Buffer().readFrom(it.toInputStream())
            SourceFetchResult(ImageSource(buffer, FileSystem.SYSTEM), null, DataSource.NETWORK)
        }
    }.first()

    class Factory(private val client: RewyndClient) : Fetcher.Factory<ImageId> {
        override fun create(data: ImageId, options: Options, imageLoader: ImageLoader): Fetcher =
            RewyndClientFetcher(client, data)
    }
}