package io.rewynd.worker.scan.movie

import io.rewynd.common.model.ServerImageInfo
import io.rewynd.common.model.ServerMovieInfo

data class MovieScanResults(
    val images: Set<ServerImageInfo> = emptySet(),
    val movies: Set<ServerMovieInfo> = emptySet(),
) {
    operator fun plus(other: MovieScanResults) =
        MovieScanResults(
            images = this.images + other.images,
            movies = this.movies + other.movies
        )

    companion object {
        val EMPTY = MovieScanResults()
    }
}
