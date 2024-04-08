import { Loading } from "../../Loading";
import React, { ReactElement, useEffect, useState } from "react";
import { SeasonInfo } from "@rewynd.io/rewynd-client-typescript";
import { loadAllSeasons } from "../../../util";

export interface SeasonsLoaderProps {
  showId: string;
  onLoad: (showInfo: SeasonInfo[]) => ReactElement;
  onError?: () => void;
}

export function SeasonsLoader(props: SeasonsLoaderProps) {
  const [seasonInfos, setSeasonInfos] = useState<SeasonInfo[]>();

  useEffect(() => {
    loadAllSeasons(props.showId).then((it) => setSeasonInfos(it));
  }, [props.showId]);

  return <Loading waitFor={seasonInfos} render={(it) => props.onLoad(it)} />;
}
