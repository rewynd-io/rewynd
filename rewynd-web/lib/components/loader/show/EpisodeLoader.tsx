import { Loading } from "../../Loading";
import React, { ReactElement, useEffect, useState } from "react";
import { EpisodeInfo } from "@rewynd.io/rewynd-client-typescript";
import { HttpClient } from "../../../const";

export interface EpisodeLoaderProps {
  episodeId: string;
  onLoad: (episodeInfo: EpisodeInfo) => ReactElement;
  onError?: () => void;
}

export function EpisodeLoader(props: EpisodeLoaderProps) {
  const [episodeInfo, setEpisodeInfo] = useState<EpisodeInfo>();

  useEffect(() => {
    HttpClient.getEpisode({ episodeId: props.episodeId }).then((it) =>
      setEpisodeInfo(it),
    );
  }, [props.episodeId]);

  return (
    <Loading
      waitFor={episodeInfo}
      render={(it) =>
        it ? props.onLoad(it) : (props.onError && props.onError()) || <></>
      }
    />
  );
}
