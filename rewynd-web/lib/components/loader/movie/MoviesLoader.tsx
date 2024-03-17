import React, { ReactElement, useEffect, useState } from "react";
import { MovieInfo } from "@rewynd.io/rewynd-client-typescript";
import { HttpClient } from "../../../const";
import { Loading } from "../../Loading";

export interface MoviesLoaderProps {
  libraryId: string;
  onLoad: (movies: MovieInfo[]) => ReactElement;
  onError?: () => void;
}

export function MoviesLoader(props: MoviesLoaderProps) {
  const [movies, setMovies] = useState<MovieInfo[]>();

  useEffect(() => {
    HttpClient.listMovies({ libraryId: props.libraryId }).then(
      (it: MovieInfo[]) => setMovies(it),
    );
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
