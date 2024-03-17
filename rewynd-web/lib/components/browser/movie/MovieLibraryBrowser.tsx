import React from "react";
import { ButtonLink } from "../../ButtonLink";
import { useParams } from "react-router";
import { Box, Grid, Typography } from "@mui/material";
import { WebRoutes } from "../../../routes";
import { NavBar } from "../../NavBar";
// import { ApiImage } from "../../Image";
import { MoviesLoader } from "../../loader/movie/MoviesLoader";

export function MovieLibraryBrowser() {
  const library = useParams()["libraryId"];
  if (!library) return <></>;

  return (
    <NavBar>
      <MoviesLoader
        libraryId={library}
        onLoad={(movies) => (
          <Grid container direction={"row"} key={`MoviesContainer-${library}`}>
            {movies
              .sort((a, b) => {
                console.log(`${a.title} ${b.title}`);
                console.log(`${a} ${b}`);
                return a.title.localeCompare(b.title);
              })
              .map((movieInfo) => {
                return (
                  <Grid
                    item
                    key={`MoviesContainer-${movieInfo.id}`}
                    xs={12}
                    sm={6}
                    md={4}
                    lg={3}
                    xl={2}
                  >
                    <ButtonLink
                      key={movieInfo.id}
                      to={WebRoutes.formatMovieRoute(movieInfo.id)}
                      sx={{ width: "100%" }}
                    >
                      <Box sx={{ width: "100%" }}>
                        {/*<ApiImage*/}
                        {/*  id={showEpisodeInfo}*/}
                        {/*  style={{ width: "100%" }}*/}
                        {/*  alt={showEpisodeInfo.title}*/}
                        {/*></ApiImage>{" "}*/}
                        <Typography align={"center"}>
                          {movieInfo.title}
                        </Typography>
                      </Box>
                    </ButtonLink>
                  </Grid>
                );
              })}
          </Grid>
        )}
      />
    </NavBar>
  );
}
