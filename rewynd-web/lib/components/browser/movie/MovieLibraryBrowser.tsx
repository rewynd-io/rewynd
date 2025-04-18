import React from "react";
import { ButtonLink } from "../../ButtonLink";
import { useParams } from "react-router";
import { Box, Grid, Typography } from "@mui/material";
import { WebRoutes } from "../../../routes";
import { NavBar } from "../../NavBar";
import { MoviesLoader } from "../../loader/movie/MoviesLoader";
import { cardWidth } from "../../../const";
import { ApiImage } from "../../Image";

export function MovieLibraryBrowser() {
  const library = useParams()["libraryId"];
  if (!library) return <></>;

  return (
    <NavBar>
      <MoviesLoader
        libraryId={library}
        onLoad={(shows) => (
          <Grid container direction={"row"} key={library}>
            {shows
              .sort((a, b) => {
                console.log(`${a.title} ${b.title}`);
                console.log(`${a} ${b}`);
                return a.title.localeCompare(b.title);
              })
              .map((movieInfo) => {
                return (
                  <Grid
                    key={movieInfo.id}
                    size={{ xs: 12, sm: 6, md: 4, lg: 3, xl: 2 }}
                  >
                    <ButtonLink
                      key={movieInfo.id}
                      to={WebRoutes.formatMovieRoute(movieInfo.id)}
                      sx={{
                        height: "100%",
                        width: cardWidth,
                      }}
                    >
                      <ApiImage
                        id={movieInfo.posterImageId}
                        style={{ width: "100%", height: "100%" }}
                        sx={{ width: "100%", height: "100%" }}
                        alt={movieInfo.title}
                      >
                        <Box
                          sx={{
                            position: "absolute",
                            bottom: "0px",
                            background: "rgba(0, 0, 0, 0.75)",
                            width: "100%",
                          }}
                        >
                          <Typography align={"center"}>
                            {movieInfo.title}
                          </Typography>
                        </Box>
                      </ApiImage>
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
