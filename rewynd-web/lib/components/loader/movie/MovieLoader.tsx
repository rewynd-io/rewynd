import React, { ReactElement, useEffect, useState } from "react";
import { MovieInfo } from "@rewynd.io/rewynd-client-typescript";
import { Loading } from "../../Loading";
import { HttpClient } from "../../../const";

export interface MovieLoaderProps {
  movieId: string;
  onLoad: (movieInfo: MovieInfo) => ReactElement;
  onError?: () => void;
}

export function MovieLoader(props: MovieLoaderProps) {
  const [movieInfo, setMovieInfo] = useState<MovieInfo>();

  useEffect(() => {
    HttpClient.getMovie({ movieId: props.movieId }).then((it: MovieInfo) =>
      setMovieInfo(it),
    );
  }, [props.movieId]);

  return (
    <Loading
      waitFor={movieInfo}
      render={(it) =>
        it ? props.onLoad(it) : (props.onError && props.onError()) || <></>
      }
    />
  );
}
