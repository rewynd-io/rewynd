import { Library } from "@rewynd.io/rewynd-client-typescript";
import { Loading } from "../Loading";
import React, { ReactElement, useEffect, useState } from "react";
import { HttpClient } from "../../const";

export interface LibraryLoaderProps {
  onLoad: (libraries: Library[]) => ReactElement;
  onError?: () => void;
}

export function LibraryLoader(props: LibraryLoaderProps) {
  const [libraries, setLibraries] = useState<Library[]>();

  useEffect(() => {
    HttpClient.listLibraries().then((it) => setLibraries(it));
  }, []);

  return (
    <Loading
      waitFor={libraries}
      render={(it) => {
        return props.onLoad(it);
      }}
    />
  );
}
