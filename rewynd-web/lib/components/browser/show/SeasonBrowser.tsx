import React from "react";

import { useParams } from "react-router";
import { EpisodesLoader } from "../../loader/show/EpisodesLoader";
import { Box, Grid } from "@mui/material";
import { NavBar } from "../../NavBar";
import { List } from "immutable";
import { EpisodeCard } from "../card/EpisodeCard";

export function SeasonBrowser() {
  const season = useParams()["seasonId"];
  if (!season) return <></>;
  return (
    <NavBar>
      <EpisodesLoader
        seasonId={season}
        onLoad={(episodes) => {
          return (
            <Grid
              container
              direction={"row"}
              sx={{ height: "100%" }}
              key={`EpisodesContainer-${season}`}
            >
              {List(episodes)
                .sortBy((it) => it.episode)
                .map((showEpisodeInfo) => {
                  // TODO fetch progress to be displayed on the cards
                  return (
                    <Box sx={{ minHeight: "10em" }} key={showEpisodeInfo.id}>
                      <EpisodeCard episode={showEpisodeInfo} />
                    </Box>
                  );
                })}
            </Grid>
          );
        }}
      />
    </NavBar>
  );
}
