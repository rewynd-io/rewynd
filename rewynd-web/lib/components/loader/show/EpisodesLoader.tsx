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
    HttpClient.listEpisodes({ seasonId: props.seasonId }).then((it) =>
      setEpisodeInfos(it),
    );
  }, [props.seasonId]);

  return <Loading waitFor={episodeInfos} render={(it) => props.onLoad(it)} />;
}
