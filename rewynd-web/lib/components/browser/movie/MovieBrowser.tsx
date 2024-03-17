import React from "react";
import { useParams } from "react-router";
import { Typography } from "@mui/material";
import { NavBar } from "../../NavBar";
import { WebRoutes } from "../../../routes";
import { ButtonLink } from "../../ButtonLink";
import { ApiImage } from "../../Image";
import { MovieLoader } from "../../loader/movie/MovieLoader";

export function MovieBrowser() {
  const movieId = useParams()["movieId"];
  if (!movieId) return <></>;

  return (
    <NavBar>
      <MovieLoader
        movieId={movieId}
        onLoad={(movie) => {
          return (
            <>
              <Typography>{movie.title}</Typography>
              <ButtonLink to={WebRoutes.Player.formatMovieRoute(movie.id)}>
                <ApiImage
                  id={movie.episodeImageId}
                  style={{ width: "100%" }}
                  alt={movie.title}
                ></ApiImage>{" "}
                <Typography align={"center"}>{movie.title}</Typography>
              </ButtonLink>
            </>
          );
        }}
      />
    </NavBar>
  );
}
