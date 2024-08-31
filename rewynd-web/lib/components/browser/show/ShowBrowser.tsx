import React from "react";
import { ButtonLink } from "../../ButtonLink";
import { useParams } from "react-router";
import { SeasonsLoader } from "../../loader/show/SeasonsLoader";
import { Box, Grid2 as Grid, Stack, Typography } from "@mui/material";
import { WebRoutes } from "../../../routes";
import { NavBar } from "../../NavBar";
import { List } from "immutable";
import { ApiImage } from "../../Image";

export function ShowBrowser() {
  const show = useParams()["showId"];
  if (!show) return <></>;

  return (
    <NavBar>
      <SeasonsLoader
        showId={show}
        onLoad={({ seasonInfos, showInfo }) => {
          return (
            <Stack direction={"column"}>
              <Stack direction={"row"}>
                <ApiImage
                  style={{ width: "30%" }}
                  alt={`${showInfo.title} Series Image`}
                  id={showInfo.seriesImageId}
                />
                <Stack direction={"column"}>
                  <Typography>{showInfo.title}</Typography>
                  <Typography>{showInfo.plot ?? showInfo.outline}</Typography>
                </Stack>
              </Stack>
              <Grid
                container
                direction={"row"}
                key={`SeasonsContainer-${show}`}
              >
                {List(seasonInfos)
                  .sortBy((it) => it.seasonNumber)
                  .map((showSeasonInfo) => {
                    return (
                      <Grid
                        size={{ xs: 2 }}
                        key={`SeasonContainer-${showSeasonInfo.id}`}
                      >
                        <ButtonLink
                          key={showSeasonInfo.id}
                          to={WebRoutes.formatSeasonRoute(showSeasonInfo.id)}
                          sx={{
                            width: "100%",
                            height: "100%",
                            minHeight: "20em",
                          }}
                        >
                          <Box sx={{ width: "100%", height: "100%" }}>
                            <ApiImage
                              id={showSeasonInfo.folderImageId}
                              style={{ width: "100%", height: "100%" }}
                              alt={`Season ${showSeasonInfo.seasonNumber}`}
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
                                  {`Season ${showSeasonInfo.seasonNumber}`}
                                </Typography>
                              </Box>
                            </ApiImage>
                          </Box>
                        </ButtonLink>
                      </Grid>
                    );
                  })}
              </Grid>
            </Stack>
          );
        }}
      />
    </NavBar>
  );
}
