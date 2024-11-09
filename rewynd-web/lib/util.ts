import {
  EpisodeInfo,
  MediaInfo,
  MovieInfo,
  Progress,
  ResponseError,
} from "@rewynd.io/rewynd-client-typescript";
import {
  instanceOfEpisodeInfo,
  instanceOfMovieInfo,
} from "@rewynd.io/rewynd-client-typescript";
import { HttpClient } from "./const";
import { SerializableEpisodeInfo } from "./models";

export type Nil = undefined | null | void;

export function isNotNil<T>(t: T | Nil): t is T {
  return !isNil(t);
}

export function isNil<T>(t: T | Nil): t is Nil {
  return t === undefined || t === null;
}

export function formatSeason(seasonNumber: number) {
  const ceilSeason = Math.ceil(seasonNumber);
  return ceilSeason < 10 ? `S0${ceilSeason}` : `S${ceilSeason}`;
}

export function formatEpisode(
  episodeStartNumber: number,
  episodeEndNumber?: number,
) {
  const ceilEpStart = Math.ceil(episodeStartNumber);
  const ceilEpEnd = episodeEndNumber ? Math.ceil(episodeEndNumber) : undefined;
  return ceilEpEnd
    ? ceilEpEnd < 10
      ? ceilEpStart < 10
        ? `E0${ceilEpStart}-E0${ceilEpEnd}`
        : `E${ceilEpStart}-E0${ceilEpEnd}`
      : ceilEpStart < 10
        ? `E0${ceilEpStart}-E${ceilEpEnd}`
        : `E${ceilEpStart}-E${ceilEpEnd}`
    : ceilEpStart < 10
      ? `E0${ceilEpStart}`
      : `E${ceilEpStart}`;
}

export function formatSeasonEpisode(
  seasonNumber: number,
  episodeStartNumber: number,
  episodeEndNumber?: number,
) {
  const seasonStr = formatSeason(seasonNumber);
  const epStr = formatEpisode(episodeStartNumber, episodeEndNumber);

  return `${seasonStr}${epStr}`;
}

export function mediaInfoTitle(mediaInfo: MediaInfo): string {
  if (isEpisodeInfo(mediaInfo)) {
    return `${mediaInfo.showName} - ${formatSeasonEpisode(
      mediaInfo.season ?? 0,
      mediaInfo.episode ?? 0,
      mediaInfo.episodeNumberEnd,
    )} - ${mediaInfo.title}`;
  } else if (isMovieInfo(mediaInfo)) {
    return mediaInfo.title;
  } else {
    return mediaInfo.id;
  }
}

function isEpisodeInfo(obj: unknown): obj is EpisodeInfo {
  return !!obj && typeof obj === "object" && instanceOfEpisodeInfo(obj);
}
function isMovieInfo(obj: unknown): obj is MovieInfo {
  return !!obj && typeof obj === "object" && instanceOfMovieInfo(obj);
}

export function resetCompletedProgress(progress: Progress): Progress {
  return progress.percent <= 0.99
    ? progress
    : { id: progress.id, percent: 0, timestamp: new Date() };
}

export function isResponseError(obj: unknown): obj is ResponseError {
  return (
    !!obj &&
    typeof obj === "object" &&
    "name" in obj &&
    "response" in obj &&
    obj.name === "ResponseError" &&
    !!obj.response
  );
}

export async function loadAllLibraries() {
  let cursor: string | undefined = undefined;
  const libs = [];
  do {
    const res = await HttpClient.listLibraries({
      listLibrariesRequest: { cursor: cursor },
    });
    cursor = res.cursor;
    libs.push(...res.page);
  } while (cursor);
  return libs;
}

export async function loadAllMovies(libraryId: string) {
  let cursor: string | undefined = undefined;
  const libs = [];
  do {
    const res = await HttpClient.listMovies({
      listMoviesRequest: {
        cursor: cursor,
        libraryId: libraryId,
        minProgress: 0,
        maxProgress: 1,
      },
    });
    cursor = res.cursor;
    libs.push(...res.page);
  } while (cursor);
  return libs;
}

export async function loadAllShows(libraryId: string) {
  let cursor: string | undefined = undefined;
  const libs = [];
  do {
    const res = await HttpClient.listShows({
      listShowsRequest: { cursor: cursor, libraryId: libraryId },
    });
    cursor = res.cursor;
    libs.push(...res.page);
  } while (cursor);
  return libs;
}

export async function loadAllSeasons(showId: string) {
  let cursor: string | undefined = undefined;
  const libs = [];
  do {
    const res = await HttpClient.listSeasons({
      listSeasonsRequest: { cursor: cursor, showId: showId },
    });
    cursor = res.cursor;
    libs.push(...res.page);
  } while (cursor);
  return libs;
}

export async function loadAllEpisodes(seasonId: string) {
  let cursor: string | undefined = undefined;
  const libs = [];
  do {
    const res = await HttpClient.listEpisodes({
      listEpisodesRequest: {
        cursor: cursor,
        seasonId: seasonId,
        minProgress: 0,
        maxProgress: 1,
        order: {
          property: "EpisodeId",
          sortOrder: "Ascending",
        },
      },
    });
    cursor = res.cursor;
    libs.push(...res.page);
  } while (cursor);
  return libs;
}

export async function loadAllSchedules() {
  let cursor: string | undefined = undefined;
  const schedules = [];
  do {
    const res = await HttpClient.listSchedules({
      listSchedulesRequest: { cursor: cursor },
    });
    cursor = res.cursor;
    schedules.push(...res.page);
  } while (cursor);
  return schedules;
}

export async function loadAllUsers() {
  let cursor: string | undefined = undefined;
  const users = [];
  do {
    const res = await HttpClient.listUsers({
      listUsersRequest: { cursor: cursor },
    });
    cursor = res.cursor;
    users.push(...res.page);
  } while (cursor);
  return users;
}

export async function clearUserProgress(id: string) {
  await HttpClient.putUserProgress({
    progress: {
      percent: 0,
      timestamp: new Date(),
      id,
    },
  });
}

export function stripMediaInfo(mediaInfo: MediaInfo): MediaInfo {
  return {
    runTime: mediaInfo.runTime,
    subtitleTracks: mediaInfo.subtitleTracks,
    audioTracks: mediaInfo.audioTracks,
    videoTracks: mediaInfo.videoTracks,
    libraryId: mediaInfo.libraryId,
    id: mediaInfo.id,
  };
}

export function compareEpisodeInfo(
  a: SerializableEpisodeInfo,
  b: SerializableEpisodeInfo,
) {
  if (a.episode < b.episode) {
    return -1;
  } else if (a.episode > b.episode) {
    return 1;
  } else {
    if (a.title < b.title) {
      return -1;
    } else if (a.title > b.title) {
      return 1;
    } else {
      if (a.id < b.id) {
        return -1;
      } else if (a.id > b.id) {
        return 1;
      } else {
        return 0;
      }
    }
  }
}
