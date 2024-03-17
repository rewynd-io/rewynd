/* eslint @typescript-eslint/no-namespace: 0 */

export namespace WebRoutes {
  export const root = `/`;

  export const movie = `${root}movie/:movieId`;
  export const formatMovieRoute = (movieId: string) =>
    movie.replace(":movieId", movieId);

  export const show = `${root}show/:showId`;
  export const formatShowRoute = (showId: string) =>
    show.replace(":showId", showId);

  export const season = `${root}season/:seasonId`;
  export const formatSeasonRoute = (seasonId: string) =>
    season.replace(":seasonId", seasonId);

  export const episode = `${root}episode/:episodeId`;
  export const formatEpisodeRoute = (episodeId: string) =>
    episode.replace(":episodeId", episodeId);

  export const library = `${root}library/:libraryId`;
  export const formatLibraryRoute = (libraryId: string) =>
    library.replace(":libraryId", libraryId);

  export namespace Player {
    export const root = `${WebRoutes.root}player/`;

    export const episode = `${root}episode/:episodeId`;
    export const formatEpisodeRoute = (episodeId: string) =>
      episode.replace(":episodeId", episodeId);

    export const movie = `${root}movie/:movieId`;
    export const formatMovieRoute = (movieId: string) =>
      movie.replace(":movieId", movieId);
  }
  export const settings = `${root}settings/`;

  export namespace Auth {
    export const root = `${WebRoutes.root}auth/`;
    export const login = `${root}login`;
  }
}
