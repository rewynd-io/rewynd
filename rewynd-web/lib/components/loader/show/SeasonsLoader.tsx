import { Loading } from "../../Loading";
import React, { ReactElement, useEffect, useState } from "react";
import { SeasonInfo, ShowInfo } from "@rewynd.io/rewynd-client-typescript";
import { loadAllSeasons } from "../../../util";
import { HttpClient } from "../../../const";

export interface SeasonsLoaderProps {
  showId: string;
  onLoad: (props: {
    seasonInfos: SeasonInfo[];
    showInfo: ShowInfo;
  }) => ReactElement;
  onError?: () => void;
}

export function SeasonsLoader(props: SeasonsLoaderProps) {
  const [seasonInfos, setSeasonInfos] = useState<SeasonInfo[]>();
  const [showInfo, setShowInfo] = useState<ShowInfo>();

  useEffect(() => {
    loadAllSeasons(props.showId).then((it) => setSeasonInfos(it));
    HttpClient.getShow({ showId: props.showId }).then((it) => setShowInfo(it));
  }, [props.showId]);

  return (
    <Loading
      waitFor={seasonInfos && showInfo ? { seasonInfos, showInfo } : undefined}
      render={(it) => props.onLoad(it)}
    />
  );
}
