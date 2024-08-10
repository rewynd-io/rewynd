import React, { ReactElement, useEffect, useState } from "react";
import { MovieInfo } from "@rewynd.io/rewynd-client-typescript";
import { Loading } from "../../Loading";
import { loadAllMovies } from "../../../util";

export interface MoviesLoaderProps {
  libraryId: string;
  onLoad: (movies: MovieInfo[]) => ReactElement;
  onError?: () => void;
}

export function MoviesLoader(props: MoviesLoaderProps) {
  const [movies, setMovies] = useState<MovieInfo[]>();

  useEffect(() => {
    loadAllMovies(props.libraryId).then((it) => setMovies(it));
  }, [props.libraryId]);

  return (
    <Loading
      waitFor={movies}
      render={(it) =>
        it ? props.onLoad(it) : (props.onError && props.onError()) || <></>
      }
    />
  );
}
