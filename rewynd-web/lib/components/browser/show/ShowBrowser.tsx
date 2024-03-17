import React from "react";
import { ButtonLink } from "../../ButtonLink";
import { useParams } from "react-router";
import { SeasonsLoader } from "../../loader/show/SeasonsLoader";
import { Box, Grid, Typography } from "@mui/material";
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
        onLoad={(seasons) => {
          return (
            <Grid container direction={"row"} key={`SeasonsContainer-${show}`}>
              {List(seasons)
                .sortBy((it) => it.seasonNumber)
                .map((showSeasonInfo) => {
                  return (
                    <Grid
                      item
                      key={`SeasonContainer-${showSeasonInfo.id}`}
                      xs={12}
                      sm={6}
                      md={4}
                      lg={3}
                      xl={2}
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
          );
        }}
      />
    </NavBar>
  );
}
