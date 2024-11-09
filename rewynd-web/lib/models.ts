import {
  SeasonInfo,
  EpisodeInfo,
  Library,
  Progress,
} from "@rewynd.io/rewynd-client-typescript";

export interface PropsWithLibrary {
  library: Library;
}

export type SerializableSeasonInfo = Omit<
  SeasonInfo,
  "premiered" | "releaseDate"
> & {
  premiered: string | undefined;
  releaseDate: string | undefined;
};

export function toSerializableSeasonInfo(
  seasonInfo: SeasonInfo,
): SerializableSeasonInfo {
  return {
    ...seasonInfo,
    premiered: seasonInfo.premiered?.toISOString(),
    releaseDate: seasonInfo.releaseDate?.toISOString(),
  };
}

export function fromSerializableSeasonInfo(
  serializableSeasonInfo: SerializableSeasonInfo,
): SeasonInfo {
  return {
    ...serializableSeasonInfo,
    premiered: serializableSeasonInfo.premiered
      ? new Date(serializableSeasonInfo.premiered)
      : undefined,
    releaseDate: serializableSeasonInfo.releaseDate
      ? new Date(serializableSeasonInfo.releaseDate)
      : undefined,
  };
}

export type SerializableEpisodeInfo = Omit<
  EpisodeInfo,
  "lastModified" | "lastUpdated" | "aired" | "progress"
> & {
  lastModified: string;
  lastUpdated: string;
  aired?: string;
  progress: SerializableProgress;
};

export function toSerializableEpisodeInfo(
  episodeInfo: EpisodeInfo,
): SerializableEpisodeInfo {
  return {
    ...episodeInfo,
    lastModified: episodeInfo.lastModified.toISOString(),
    lastUpdated: episodeInfo.lastUpdated.toISOString(),
    aired: episodeInfo.aired ? episodeInfo.aired.toISOString() : undefined,
    progress: toSerializableProgress(episodeInfo.progress),
  };
}

export function fromSerializableEpisodeInfo(
  serializableEpisodeInfo: SerializableEpisodeInfo,
): EpisodeInfo {
  return {
    ...serializableEpisodeInfo,
    lastModified: new Date(serializableEpisodeInfo.lastUpdated),
    lastUpdated: new Date(serializableEpisodeInfo.lastUpdated),
    aired: serializableEpisodeInfo.aired
      ? new Date(serializableEpisodeInfo.aired)
      : undefined,
    progress: fromSerializableProgress(serializableEpisodeInfo.progress),
  };
}

export type SerializableProgress = Omit<Progress, "timestamp"> & {
  timestamp: string;
};

export function toSerializableProgress(
  progress: Progress,
): SerializableProgress {
  return {
    ...progress,
    timestamp: progress.timestamp.toISOString(),
  };
}

export function fromSerializableProgress(
  serializableProgress: SerializableProgress,
): Progress {
  return {
    ...serializableProgress,
    timestamp: new Date(serializableProgress.timestamp),
  };
}
