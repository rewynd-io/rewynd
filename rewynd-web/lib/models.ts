import {
  EpisodeInfo,
  Library,
  Progress,
} from "@rewynd.io/rewynd-client-typescript";

export interface PropsWithLibrary {
  library: Library;
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
    lastModified: episodeInfo.lastUpdated.toISOString(),
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
