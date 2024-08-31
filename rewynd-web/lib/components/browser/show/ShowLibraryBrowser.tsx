import React from "react";
import { ButtonLink } from "../../ButtonLink";
import { useParams } from "react-router";
import { ShowsLoader } from "../../loader/show/ShowsLoader";
import { Box, Grid2 as Grid, Typography } from "@mui/material";
import { WebRoutes } from "../../../routes";
import { NavBar } from "../../NavBar";
import { ApiImage } from "../../Image";
import { cardWidth } from "../../../const";

export function ShowLibraryBrowser() {
  const library = useParams()["libraryId"];
  if (!library) return <></>;

  return (
    <NavBar>
      <ShowsLoader
        libraryId={library}
        onLoad={(shows) => (
          <Grid container direction={"row"} key={`SeriesContainer-${library}`}>
            {shows
              .sort((a, b) => {
                console.log(`${a.title} ${b.title}`);
                console.log(`${a} ${b}`);
                return a.title.localeCompare(b.title);
              })
              .map((showEpisodeInfo) => {
                return (
                  <Grid
                    size={{ xs: 12, sm: 6, md: 4, lg: 3, xl: 2 }}
                    key={`SeriesContainer-${showEpisodeInfo.id}`}
                  >
                    <ButtonLink
                      key={showEpisodeInfo.id}
                      to={WebRoutes.formatShowRoute(showEpisodeInfo.id)}
                      sx={{
                        height: "100%",
                        width: cardWidth,
                      }}
                    >
                      <ApiImage
                        id={showEpisodeInfo.seriesImageId}
                        style={{ width: "100%", height: "100%" }}
                        sx={{ width: "100%", height: "100%" }}
                        alt={showEpisodeInfo.title}
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
                            {showEpisodeInfo.title}
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
