import { Loading } from "../../Loading";
import React, { ReactElement, useEffect, useState } from "react";
import { EpisodeInfo, SeasonInfo } from "@rewynd.io/rewynd-client-typescript";
import { HttpClient } from "../../../const";
import { loadAllEpisodes } from "../../../util";

export interface EpisodesLoaderProps {
  seasonId: string;
  onLoad: (props: {
    seasonInfo: SeasonInfo;
    episodeInfos: EpisodeInfo[];
  }) => ReactElement;
  onError?: () => void;
}

export function EpisodesLoader(props: EpisodesLoaderProps) {
  const [episodeInfos, setEpisodeInfos] = useState<EpisodeInfo[]>();
  const [seasonInfo, setSeasonInfo] = useState<SeasonInfo>();

  useEffect(() => {
    (async () => {
      setEpisodeInfos(await loadAllEpisodes(props.seasonId));
      setSeasonInfo(await HttpClient.getSeasons({ seasonId: props.seasonId }));
    })();
  }, [props.seasonId]);

  return (
    <Loading
      waitFor={
        episodeInfos && seasonInfo ? { episodeInfos, seasonInfo } : undefined
      }
      render={(it) => props.onLoad(it)}
    />
  );
}
