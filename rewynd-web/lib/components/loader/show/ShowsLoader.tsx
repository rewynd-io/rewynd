import { ShowInfo } from "@rewynd.io/rewynd-client-typescript";
import { Loading } from "../../Loading";
import React, { ReactElement, useEffect, useState } from "react";
import { HttpClient } from "../../../const";

export interface ShowLibraryLoaderProps {
  libraryId: string;
  onLoad: (showInfo: ShowInfo[]) => ReactElement;
  onError?: () => void;
}

export function ShowsLoader(props: ShowLibraryLoaderProps) {
  const [showInfos, setShowInfos] = useState<ShowInfo[]>();

  useEffect(() => {
    HttpClient.listShows({ libraryId: props.libraryId }).then((it) =>
      setShowInfos(it),
    );
  }, [props.libraryId]);

  return (
    <Loading
      waitFor={showInfos}
      render={(it) => {
        console.log(JSON.stringify(it));
        return props.onLoad(it);
      }}
    />
  );
}
