import { Loading } from "../../Loading";
import React, { ReactElement, useEffect, useState } from "react";
import { EpisodeInfo } from "@rewynd.io/rewynd-client-typescript";
import { HttpClient } from "../../../const";

export interface EpisodesLoaderProps {
  seasonId: string;
  onLoad: (episodeInfos: EpisodeInfo[]) => ReactElement;
  onError?: () => void;
}

export function EpisodesLoader(props: EpisodesLoaderProps) {
  const [episodeInfos, setEpisodeInfos] = useState<EpisodeInfo[]>();

  useEffect(() => {
    (async () => {
      let cursor: string | undefined = undefined;
      const episodes: EpisodeInfo[] = [];
      do {
        const res = await HttpClient.listEpisodes({
          listEpisodesRequest: {
            seasonId: props.seasonId,
            cursor: cursor,
          },
        });
        cursor = res.cursor;
        episodes.push(...res.page);
      } while (cursor);
      setEpisodeInfos(episodes);
    })();
  }, [props.seasonId]);

  return <Loading waitFor={episodeInfos} render={(it) => props.onLoad(it)} />;
}
